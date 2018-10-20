/*
 * Copyright 2017 The Android Things Samples Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.imageclassifier;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidthings.imageclassifier.classifier.Recognition;
import com.example.androidthings.imageclassifier.classifier.TensorFlowHelper;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;


import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ImageClassifierActivity extends Activity {
    private static final String TAG = "ImageClassifierActivity";

    /** Camera image capture size */
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;
    /** Image dimensions required by TF model */
    private static final int TF_INPUT_IMAGE_WIDTH = 224;
    private static final int TF_INPUT_IMAGE_HEIGHT = 224;
    /** Dimensions of model inputs. */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    /** TF model asset files */
    private static String LABELS_FILE = "labels.txt";
//    private static String MODEL_FILE = "mobilenet_quant_v1_224.tflite";
    private static String MODEL_FILE =  "graph.tflite";

    private ButtonInputDriver mButtonDriver;
    private boolean mProcessing;

    private ImageView mImage;
    private TextView mResultText;

    // TODO: ADD ARTIFICIAL INTELLIGENCE
    private Interpreter mTensorFlowLite;
    private List<String> mLabels;

    // TODO: ADD CAMERA SUPPORT
    private CameraHandler mCameraHandler;
    private ImagePreprocessor mImagePreprocessor;


    private ButtonInputDriver mButtonBDriver;
    private I2cDevice i2cDevice;
    private ButtonInputDriver mButtonADriver;
    private int MAX_RESULTS = 3;

    /**
     * Initialize the classifier that will be used to process images.
     */
    private void initClassifier() {
        try {
            mTensorFlowLite =
                    new Interpreter(TensorFlowHelper.loadModelFile(this, MODEL_FILE));
            mLabels = TensorFlowHelper.readLabels(this, LABELS_FILE);
        } catch (IOException e) {
            Log.w(TAG, "Unable to initialize TensorFlow Lite.", e);
        }
    }

    /**
     * Clean up the resources used by the classifier.
     */
    private void destroyClassifier() {
        // TODO: ADD ARTIFICIAL INTELLIGENCE
        mTensorFlowLite.close();
    }

    /**
     * Process an image and identify what is in it. When done, the method
     * {@link #onPhotoRecognitionReady(Collection)} must be called with the results of
     * the image recognition process.
     *
     * @param image Bitmap containing the image to be classified. The image can be
     *              of any size, but preprocessing might occur to resize it to the
     *              format expected by the classification process, which can be time
     *              and power consuming.
     */
    private void doRecognize(Bitmap image) {
        // TODO: ADD ARTIFICIAL INTELLIGENCE
        // Allocate space for the inference results
        float[][] confidencePerLabel = new float[1][mLabels.size()];
        // Allocate buffer for image pixels.
        int[] intValues = new int[TF_INPUT_IMAGE_WIDTH * TF_INPUT_IMAGE_HEIGHT];
        ByteBuffer imgData = ByteBuffer.allocateDirect(
                4 * DIM_BATCH_SIZE * TF_INPUT_IMAGE_WIDTH * TF_INPUT_IMAGE_HEIGHT * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());

        // Read image data into buffer formatted for the TensorFlow model
        TensorFlowHelper.convertBitmapToByteBuffer(image, intValues, imgData);

        // Run inference on the network with the image bytes in imgData as input,
        // storing results on the confidencePerLabel array.
        mTensorFlowLite.run(imgData, confidencePerLabel);

        // Get the results with the highest confidence and map them to their labels
        Collection<Recognition> results =
                TensorFlowHelper.getBestResults(confidencePerLabel, mLabels);

        // Report the results with the highest confidence
        onPhotoRecognitionReady(results);
    }

    /**
     * Initialize the camera that will be used to capture images.
     */
    private void initCamera() {
        // TODO: ADD CAMERA SUPPORT
        mImagePreprocessor = new ImagePreprocessor(
                PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT,
                TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT);
        mCameraHandler = CameraHandler.getInstance();
        mCameraHandler.initializeCamera(this,
                PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, null,
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        Bitmap bitmap = mImagePreprocessor.preprocessImage(imageReader.acquireNextImage());
                        onPhotoReady(bitmap);
                    }
                });
    }

    /**
     * Clean up resources used by the camera.
     */
    private void closeCamera() {
        // TODO: ADD CAMERA SUPPORT
        mCameraHandler.shutDown();
    }

    /**
     * Load the image that will be used in the classification process.
     * When done, the method {@link #onPhotoReady(Bitmap)} must be called with the image.
     */
    private void loadPhoto() {
        // TODO: ADD CAMERA SUPPORT
        Bitmap bitmap = getStaticBitmap();
        onPhotoReady(bitmap);
    }



    // --------------------------------------------------------------------------------------
    // NOTE: The normal codelab flow won't require you to change anything below this line,
    // although you are encouraged to read and understand it.

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);
        mImage = findViewById(R.id.imageView);
        mResultText = findViewById(R.id.resultText);

        updateStatus(getString(R.string.initializing));
        initCamera();
        initClassifier();
        initButton();
        updateStatus(getString(R.string.help_message));
    }

    /**
     * Register a GPIO button that, when clicked, will generate the {@link KeyEvent#KEYCODE_ENTER}
     * key, to be handled by {@link #onKeyUp(int, KeyEvent)} just like any regular keyboard
     * event.
     *
     * If there's no button connected to the board, the doRecognize can still be triggered by
     * sending key events using a USB keyboard or `adb shell input keyevent 66`.
     */
    private void initButton() {
        try {
            mButtonDriver = RainbowHat.createButtonCInputDriver(KeyEvent.KEYCODE_ENTER);
            mButtonDriver.register();

            mButtonBDriver = RainbowHat.createButtonBInputDriver(KeyEvent.KEYCODE_BUTTON_1);
            mButtonBDriver.register();

            mButtonADriver = RainbowHat.createButtonAInputDriver(KeyEvent.KEYCODE_BUTTON_2);
            mButtonADriver.register();
        } catch (IOException e) {
            Log.w(TAG, "Cannot find button. Ignoring push button. Use a keyboard instead.", e);
        }
    }

    private Bitmap getStaticBitmap() {
        Log.d(TAG, "Using sample photo in res/drawable/sampledog_224x224.png");
        return BitmapFactory.decodeResource(this.getResources(), R.drawable.sampledog_224x224);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        //TODO: proper enum for buttons
        switch (keyCode) {

            case KeyEvent.KEYCODE_ENTER: {
                Log.d(TAG, "The button C was pressed");
                if (mProcessing) {
                    updateStatus("Still processing, please wait");
                    return true;
                }
                updateStatus("Running photo recognition");
                mProcessing = true;
                loadPhoto();

                break;
            }

            case KeyEvent.KEYCODE_BUTTON_1: {
                Log.d(TAG, "The button B was pressed");

                updateStatus("Just received a new fancy model. Updating..");
                this.MODEL_FILE = "food.tflite";
                this.LABELS_FILE = "food-labels.txt";
                this.MAX_RESULTS = 1;

                this.initClassifier();


                PeripheralManager manager = PeripheralManager.getInstance();
                List<String> deviceList = manager.getI2cBusList();
                if (deviceList.isEmpty()) {
                    Log.i(TAG, "No I2C bus available on this device.");
                } else {
                    Log.i(TAG, "List of available devices: " + deviceList);
                }

                // Attempt to access the I2C device
                String I2C_DEVICE_NAME = "I2C1";
                int I2C_ADDRESS = 0x55;

//                performScan(manager, I2C_DEVICE_NAME);
                // I2C1: 0x55, 0x70, 0x77
                // I2C2: 0x50


                try {
                    if (i2cDevice == null) {
                        i2cDevice = manager.openI2cDevice(I2C_DEVICE_NAME, I2C_ADDRESS);
                    }

                    Log.i(TAG, "i2C device: " + i2cDevice);
                } catch (IOException e) {
                    Log.w(TAG, "Unable to access I2C device", e);
                }

                try {
                    byte[] data = readCalibration(i2cDevice, 0x0);

                    Log.w(TAG, "Here is data:  " + new String(data, StandardCharsets.UTF_8));

                    StringBuffer result = new StringBuffer();
                    for (byte b : data) {
                        result.append(String.format("%02X ", b));
//                        result.append(""); // delimiter
                    }
                    Log.w(TAG, "Here is data:  " + result.toString());
                } catch (IOException e) {
                    Log.w(TAG, "Unable to read data: " + e.getMessage());
                }

                break;
            }
            case KeyEvent.KEYCODE_BUTTON_2: {
                Log.d(TAG, "The button A was pressed");

                mCameraHandler.takePicture();
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    // Read a register block
    public byte[] readCalibration(I2cDevice device, int startAddress) throws IOException {
        // Read three consecutive register values
        byte[] data = new byte[8];
        device.readRegBuffer(startAddress, data, data.length);
        return data;
    }

    private void performScan(PeripheralManager manager, String i2c_bus) {
        for (int address = 0; address < 256; address++) {

            //auto-close the devices
            try (final I2cDevice device = manager.openI2cDevice(i2c_bus, address)) {

                try {
                    device.readRegByte(0x0);
                    Log.i(TAG, String.format(Locale.US, "Trying: 0x%02X - SUCCESS", address));
                } catch (final IOException e) {
                    Log.i(TAG, String.format(Locale.US, "Trying: 0x%02X - FAIL", address));
                }

            } catch (final IOException e) {
                //in case the openI2cDevice(name, address) fails
            }
        }
    }

    /**
     * Image capture process complete
     */
    private void onPhotoReady(Bitmap bitmap) {
        mImage.setImageBitmap(bitmap);
        doRecognize(bitmap);
    }

    /**
     * Image classification process complete
     */
    private void onPhotoRecognitionReady(Collection<Recognition> results) {
        updateStatus(formatResults(results, MAX_RESULTS));
        mProcessing = false;
    }

    /**
     * Format results list for display
     */
    private String formatResults(Collection<Recognition> results, int maxResults) {
        if (results == null || results.isEmpty()) {
            return getString(R.string.empty_result);
        } else {
            StringBuilder sb = new StringBuilder();
            Iterator<Recognition> it = results.iterator();
            int counter = 0;
            while (it.hasNext() && counter < maxResults) {
                Recognition r = it.next();
                sb.append(r.getTitle());
                counter++;
                if (counter < results.size() - 1) {
                    sb.append(", ");
                } else if (counter == results.size() - 1 && maxResults > 1) {
                    sb.append(" or ");
                }
            }

            return sb.toString();
        }
    }

    /**
     * Report updates to the display and log output
     */
    private void updateStatus(String status) {
        Log.d(TAG, status);
        mResultText.setText(status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            destroyClassifier();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            closeCamera();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mButtonDriver != null) mButtonDriver.close();
        } catch (Throwable t) {
            // close quietly
        }


        if (i2cDevice != null) {
            try {
                i2cDevice.close();
                i2cDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close I2C device", e);
            }
        }
    }
}
