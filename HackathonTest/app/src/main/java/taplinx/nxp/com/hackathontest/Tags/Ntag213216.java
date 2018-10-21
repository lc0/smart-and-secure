package taplinx.nxp.com.hackathontest.Tags;

import android.util.Log;

import com.nxp.nfclib.ndef.INdefMessage;
import com.nxp.nfclib.ndef.NdefMessageWrapper;
import com.nxp.nfclib.ndef.NdefRecordWrapper;
import com.nxp.nfclib.ntag.INTag213215216;
import com.nxp.nfclib.ntag.NTag210;
import com.nxp.nfclib.utils.Utilities;

import taplinx.nxp.com.hackathontest.NTAGInterface;

/**
 * Created by nxf41757 on 16.10.2018.
 */

public class Ntag213216 implements NTAGInterface {

    /**
     * UID => 7bytes
     * Counter => 3bytes
     */

    private final String TAG_NDEF_PATH = "https://ntag.nxp.com/213?m=00000000000000x000000&s=";
    private final String TAG = "NTAG213216";

    INTag213215216 tag;
    boolean isNtag213;

    public Ntag213216(INTag213215216 tag, boolean isNtag213) {
        this.tag = tag;
        this.isNtag213 = isNtag213;
    }

    public String readNDEF() {
        INdefMessage imessage = tag.readNDEF();

        return imessage.toString();
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

        // Reads the 16 bytes of data from the given page address.
        byte[] data = tag.read((byte) 0x04);

        Log.i(TAG, Utilities.dumpBytes(data));

        enableMirroring();
        String completeNDEF = TAG_NDEF_PATH + getSignature();
        NdefRecordWrapper ndefRecordWrapper = NdefRecordWrapper.createUri(completeNDEF);
        NdefMessageWrapper ndefMessageWrapper = new NdefMessageWrapper(ndefRecordWrapper);
        tag.writeNDEF(ndefMessageWrapper);
    }

    /**
     * Automatically reads UID and Counter from Tag
     * enableMirroring(NTag210.MirrorType mirrorType,int mirrorPageAdd,byte byteposInMirrorPage)
     * mirrorPageAdd - Mirror Page Address defines the page for the beginning of the ASCII mirroring
     * byteposInMirrorPage - Defines the byte position within the page defined by mirrorPageAdd ( beginning of ASCII mirror )
     */
    private void enableMirroring() {
        if (isNtag213) {
            tag.enableCounter(true);
            tag.enableMirroring(NTag210.MirrorType.UID_NFC_CNT_ASCII_MIRROR, 0x0A, (byte) 2);
        } else {
            // For NTAG216 Try the same code as for NTAG213, if not working set mirroring manually
            byte[] cfgPages = tag.read(0xE3);
            tag.writeConfigBytes(
                    new byte[]{
                            (byte) 0xE4, (byte) 0x00, (byte) 0x0A, (byte) 0xFF,
                            (byte) (cfgPages[4] | 0x10), cfgPages[5], cfgPages[6], cfgPages[7]
                    });
        }
    }

    /**
     * Originality check
     * With this feature, it is
     * possible to verify with a certain confidence that the tag is using an IC manufactured by
     * NXP Semiconductors.
     *
     * @return String signature
     */
    private String getSignature() {
        return Utilities.dumpBytes(tag.readSignature()).split("x", 2)[1];
    }
}
