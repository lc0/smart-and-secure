package taplinx.nxp.com.hackathontest.Tags;

import android.util.Log;

import com.nxp.nfclib.defaultimpl.KeyData;
import com.nxp.nfclib.desfire.INTag413DNA;
import com.nxp.nfclib.desfire.NTag413DNAFileSettings;
import com.nxp.nfclib.ndef.NdefMessageWrapper;
import com.nxp.nfclib.ndef.NdefRecordWrapper;
import com.nxp.nfclib.utils.Utilities;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import taplinx.nxp.com.hackathontest.NTAGInterface;

/**
 * Created by nxf41757 on 16.10.2018.
 */

public class Ntag413DNA implements NTAGInterface {

    /**
     * UID => 7bytes
     * Counter => 3Bytes
     * CMAC => 8bytes
     */

    private final String TAG_NDEF_PATH = "https://ntag.nxp.com/413?m=00000000000000x000000&c=0000000000000000";
    private final String TAG = "NTAG413DNA";
    // file identifiers
    private final byte[] DF = new byte[]{(byte) 0xD2, 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01, (byte) 0x01};
    private final byte[] FILE_NDEF_ID = new byte[]{(byte) 0x04, (byte) 0xE1};
    private final byte[] FILE_CC_ID = new byte[]{(byte) 0x03, (byte) 0xE1};

    public final byte[] DEFAULT_KEY_AES =
            {                                                 // Default AES128 key
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00
            };

    private byte[] data = new byte[(byte) 0x00];

    private INTag413DNA tag;

    public Ntag413DNA(INTag413DNA tag) {
        this.tag = tag;
    }

    @Override
    public void connect() {
        if (!isConnected()) {
            tag.getReader().connect();
        }
    }

    @Override
    public void disconnect() {
        if (isConnected()) {
            tag.getReader().close();
        }
    }

    @Override
    public boolean isConnected() {
        return tag.getReader().isConnected();
    }

    @Override
    public void writeNDEF() {
        if (authenticate()) {

            // read 128 bytes
            byte[] data = tag.readData(2, 0, 128);
            Log.i(TAG, Utilities.dumpBytes(data));

            String completeNDEF = TAG_NDEF_PATH;
            NdefRecordWrapper ndefRecordWrapper = NdefRecordWrapper.createUri(completeNDEF);
            NdefMessageWrapper ndefMessageWrapper = new NdefMessageWrapper(ndefRecordWrapper);
            tag.writeNDEF(ndefMessageWrapper);
        }
    }

    public boolean authenticate() {

        try {
            Log.i(TAG, "select NDEF Application");
            selectCommand((byte) 0x04, DF); //---------------- 0x00 remake to MF, DF or EF, by file identifier

            //Log.i(TAG, "select CC File");
            //selectCommand((byte) 0x02, FILE_CC_ID);

            //Log.i(TAG, "select NDEF File");
            //selectCommand((byte) 0x02, FILE_NDEF_ID);

            KeyData keyData = new KeyData();
            Key key = new SecretKeySpec(DEFAULT_KEY_AES, "AES");
            keyData.setKey(key);
            tag.authenticateFirst(0, keyData, new byte[]{});
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * This command implements the ISO/IEC 7816-4 compatible select command.
     */
    private void selectCommand(byte address, byte[] option) {
        tag.select((byte) 0x04, false, option);
        fileSettings((byte) 0x0E, (byte) 0x0E, (byte) 0x0E, (byte) 0x0E);
    }

    /**
     * @param readAccess
     * @param writeAccess
     * @param readWriteAccess
     * @param changeAccess    0x0E - allow file to write, this should remain for changing NDEF message
     *                        0x0F - make file Read only
     */
    private void fileSettings(byte readAccess, byte writeAccess, byte readWriteAccess, byte changeAccess) {

        NTag413DNAFileSettings fileSettings = NTag413DNAFileSettings.getInstance(true, readAccess, writeAccess, readWriteAccess, changeAccess, INTag413DNA.CommunicationMode.Plain);

        fileSettings.enablePICCDataMirroring(true);
        fileSettings.enableReadNfcCounter(true);
        fileSettings.enableVCUIDMirroring(true);
        fileSettings.enableSDMCounterReadWithKey((byte) 0x0E);
        fileSettings.enableSDMFileReadWithKey((byte) 0);
        fileSettings.setUIDOffset((byte) 0x1A);
        fileSettings.setNfcCtrOffset((byte) 0x29);
        fileSettings.setMacInputOffset((byte) 0x32);
        fileSettings.setMacOffset((byte) 0x32);

        tag.changeFileSettings((byte) 0x02, fileSettings);
    }
}
