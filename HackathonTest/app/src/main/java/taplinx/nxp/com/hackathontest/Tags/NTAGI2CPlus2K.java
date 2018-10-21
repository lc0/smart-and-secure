package taplinx.nxp.com.hackathontest.Tags;

import android.util.Log;

import com.nxp.nfclib.ndef.INdefMessage;
import com.nxp.nfclib.ndef.NdefMessageWrapper;
import com.nxp.nfclib.ndef.NdefRecordWrapper;
import com.nxp.nfclib.ntag.INTAGI2Cplus;
import com.nxp.nfclib.ntag.INTag213215216;
import com.nxp.nfclib.ntag.NTag210;
import com.nxp.nfclib.utils.Utilities;

import taplinx.nxp.com.hackathontest.NTAGInterface;

/**
 * Created by nxf41757 on 16.10.2018.
 */

public class NTAGI2CPlus2K implements NTAGInterface {

    /**
     * UID => 7bytes
     * Counter => 3bytes
     */

    private final String TAG_NDEF_PATH = "https://ntag.nxp.com/213?m=00000000000000x000000&s=";
    private final String TAG = "NTAGI2CPlus2K";

    INTAGI2Cplus tag;

    public NTAGI2CPlus2K(INTAGI2Cplus tag) {
        this.tag = tag;
    }

    public byte[] readNDEF() {
        INdefMessage imessage = tag.readNDEF();

        return imessage.toByteArray();
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

        String completeNDEF = TAG_NDEF_PATH + getSignature();
        NdefRecordWrapper ndefRecordWrapper = NdefRecordWrapper.createUri(completeNDEF);
        NdefMessageWrapper ndefMessageWrapper = new NdefMessageWrapper(ndefRecordWrapper);
        tag.writeNDEF(ndefMessageWrapper);
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
