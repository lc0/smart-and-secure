package taplinx.nxp.com.hackathontest;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Highlight;
import com.github.mikephil.charting.utils.PercentFormatter;
import com.nxp.nfclib.CardType;
import com.nxp.nfclib.NxpNfcLib;
import com.nxp.nfclib.desfire.DESFireFactory;
import com.nxp.nfclib.desfire.IDESFireEV1;
import com.nxp.nfclib.ntag.NTagFactory;
import com.nxp.nfclib.utils.Utilities;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import taplinx.nxp.com.hackathontest.Tags.Desfire;
import taplinx.nxp.com.hackathontest.Tags.NTAGI2CPlus2K;
import taplinx.nxp.com.hackathontest.Tags.Ntag213216;
import taplinx.nxp.com.hackathontest.Tags.Ntag413DNA;

public class MainActivity extends AppCompatActivity {
    private String TAG = MainActivity.class.getSimpleName();
    private NxpNfcLib m_libInstance = null; // The TapLinX library instance
    private static final String LICENSE = "00112233445566778899aabbccddeeff"; // The TapLinX key
    private TextView m_textView = null;
    private IDESFireEV1 m_desfire = null;
    private PieChart chart;

    private void initializeLibrary() {
        m_libInstance = NxpNfcLib.getInstance();
        m_libInstance.registerActivity(this, LICENSE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_textView = (TextView) findViewById(R.id.mainTextView);

        chart = (PieChart) findViewById(R.id.chart);
        chart = new PieChart(this.getBaseContext());

        initializeLibrary(); // Initialize library
    }

    public void updateChart() {
        // configure pie chart
        chart.setUsePercentValues(true);
        chart.setDescription("Smartphones Market Share");

        // enable hole and configure
        chart.setDrawHoleEnabled(true);
        chart.setHoleColorTransparent(true);
        chart.setHoleRadius(7);
        chart.setTransparentCircleRadius(10);

        // enable rotation of the chart by touch
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);

//        // set a chart value selected listener
//        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
//
//            @Override
//            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
//                // display msg when value selected
//                if (e == null)
//                    return;
//
//                Toast.makeText(MainActivity.this,
//                        xData[e.getXIndex()] + " = " + e.getVal() + "%", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onNothingSelected() {
//
//            }
//        });

        // add data
        addData();

        // customize legends
        Legend l = chart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);
        l.setXEntrySpace(7);
        l.setYEntrySpace(5);
    }

    private void addData() {
        float[] yData = { 5, 10, 15, 30, 40 };
        String[] xData = { "Sony", "Huawei", "LG", "Apple", "Samsung" };

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        for (int i = 0; i < yData.length; i++)
            yVals1.add(new Entry(yData[i], i));

        ArrayList<String> xVals = new ArrayList<String>();

        for (int i = 0; i < xData.length; i++)
            xVals.add(xData[i]);

        // create pie data set
        PieDataSet dataSet = new PieDataSet(yVals1, "Market Share");
        dataSet.setSliceSpace(3);
        dataSet.setSelectionShift(5);

        // add many colors
        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());
        dataSet.setColors(colors);

        // instantiate pie data object now
        PieData data = new PieData(xVals, dataSet);

        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.GRAY);

        chart.setData(data);

        // undo all highlights
        chart.highlightValues(null);

        // update pie chart
        chart.invalidate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.i( TAG, "onStart" );

        switch (getResources().getConfiguration().orientation) {                                               // Lock screen orientation if app has stated
            case Configuration.ORIENTATION_PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onResume() {  // Called if app becomes active
        m_libInstance.startForeGroundDispatch();
        super.onResume();
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onPause() {  // Called if app becomes inactive
        m_libInstance.stopForeGroundDispatch();
        super.onPause();
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void onNewIntent(final Intent intent) {
        Log.d(TAG, "onNewIntent");
        cardLogic(intent);
        super.onNewIntent(intent);
    }

    ////////////////////////////////////////////////////////////////////////////

    private void cardLogic(final Intent intent) {
        try {
            final CardType cardType = m_libInstance.getCardType(intent);

            if (CardType.NTag213 == cardType) {
                Ntag213216 ntag213216 = new Ntag213216(NTagFactory.getInstance().getNTAG213(m_libInstance.getCustomModules()), true);
                ntag213216.connect();
                // TODO: 17.10.2018 implement logic
                ntag213216.disconnect();
            } else if (CardType.NTag216 == cardType) {
                Ntag213216 ntag213216 = new Ntag213216(NTagFactory.getInstance().getNTAG216(m_libInstance.getCustomModules()), false);
                ntag213216.connect();

                String result = ntag213216.readNDEF();
                Log.i(TAG, "NDEF: " + result);

                // TODO: 17.10.2018 implement logic
                ntag213216.disconnect();
           } else if (CardType.NTagI2CPlus2K == cardType) {
            NTAGI2CPlus2K ntag213216 = new NTAGI2CPlus2K(NTagFactory.getInstance().getNTAGI2CPlus2K(m_libInstance.getCustomModules()));
            ntag213216.connect();

            byte[] result = ntag213216.readNDEF();
            Log.i(TAG, "NDEF: " + result);

//                Utilities.dumpBytesAscii(result);
//                Utilities.dumpBytes(result)
//            Charset encoding = (result[0] & 128) == 0 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16;
//            int languageCodeLength = result[0] & 0063;

            String result_string = new String(result, 7, result.length - 7, StandardCharsets.UTF_8);

//            Log.i(TAG, "NDEF text: " + new String(result, languageCodeLength + 1, result.length - languageCodeLength - 1, encoding));

                Log.i(TAG, "NDEF text: " + result_string);

                m_textView.setText("Stored infromation: \n" + result_string);

                updateChart();

            // TODO: 17.10.2018 implement logic
            ntag213216.disconnect();
        }
            else if (CardType.NTag413DNA == cardType) {
                Ntag413DNA ntag413DNA = new Ntag413DNA(DESFireFactory.getInstance().getNTag413DNA(m_libInstance.getCustomModules()));
                ntag413DNA.connect();
                // TODO: 17.10.2018 implement logic
                ntag413DNA.disconnect();
            } else if (CardType.DESFireEV1 == cardType || CardType.DESFireEV2 == cardType) {
                Desfire desfire = new Desfire(DESFireFactory.getInstance().getDESFire(m_libInstance.getCustomModules()));
                desfire.connect();

                // TODO: 17.10.2018 implement logic

                desfire.authenticate();
                Log.i(TAG, "Auth status: " + desfire.getAuthStatus());

                int[] applicationIDs = desfire.getApplicationIDs();

                // need to authenticate with master key
                //int[] isoFileIDs = desfire.getISOFilesIDs();

                byte[] fileIDs = desfire.getFilesIDs();

                Log.d(TAG, "Card type:       " + desfire.getTagName());
                Log.d(TAG, "Android version: " + Build.VERSION.RELEASE);
                Log.d(TAG, "Phone model:     " + Build.MODEL);
                Log.d(TAG, "TapLinx version: " + m_libInstance.getTaplinxVersion());
                Log.d(TAG, "App version:     " + BuildConfig.VERSION_NAME);

                m_textView.setText("Card type: " + desfire.getTagName());
                m_textView.append("\nAndroid version: " + Build.VERSION.RELEASE);
                m_textView.append("\nPhone model: " + Build.MODEL);
                m_textView.append("\nTapLinx version: " + m_libInstance.getTaplinxVersion());
                m_textView.append("\nApp version: " + BuildConfig.VERSION_NAME);
                desfire.disconnect();
            } else {
                m_textView.setText(cardType.getTagName() + " card is not supported");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
