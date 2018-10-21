package taplinx.nxp.com.hackathontest.Tags;

import com.nxp.nfclib.defaultimpl.Crypto;
import com.nxp.nfclib.defaultimpl.KeyData;
import com.nxp.nfclib.interfaces.ICrypto;
import com.nxp.nfclib.interfaces.ICryptoGram;
import com.nxp.nfclib.ndef.NdefMessageWrapper;
import com.nxp.nfclib.ndef.NdefRecordWrapper;
import com.nxp.nfclib.ntag.INTag213TagTamper;
import com.nxp.nfclib.utils.Utilities;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import taplinx.nxp.com.hackathontest.NTAGInterface;

/**
 * Created by nxf41757 on 17.10.2018.
 */

public class Ntag213TT implements NTAGInterface {

    /**
     * UID => 7bytes
     * Counter => 3bytes
     */

    private final String TAG_NDEF_PATH = "https://ntag.nxp.com/213tt?m=00000000000000x000000x";

    private byte[] SV2 = new byte[]{
            (byte) 0x54, (byte) 0x54, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00
    };

    public final byte[] DEFAULT_KEY_AES =
            {                                                 // Default AES128 key
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00
            };

    private byte[] openState;

    private INTag213TagTamper tag;

    public Ntag213TT(INTag213TagTamper tag) {
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
        String completeNDEF = TAG_NDEF_PATH + getTTState();
        NdefRecordWrapper ndefRecordWrapper = NdefRecordWrapper.createUri(completeNDEF);
        NdefMessageWrapper ndefMessageWrapper = new NdefMessageWrapper(ndefRecordWrapper);
        try {
            tag.writeNDEF(ndefMessageWrapper);
            byte[] cfgPages = tag.read(0x29);

             /*  MIRROR_CONF:
                         *
                         *   7..5 - UID, NFC counter and TT ASCII mirror => 1 1 1
                    *   4..3 - mirror byte - 0x03       => 1 0
                    *   2..0    - RFU                      => 0 0 0
                    *   2    - strong modulation        => 1
                    *   1..0 - RFU                      => 0 0
                    *
                    *   7  6  5  4  3  2  1  0
                    *   1  1  1  0  0  0  0  0          => 0xE0
                    *
                    * 7..3 RFUI => 0 0 0 0
                    * 2 TT_LOCK => 0
                    * 1 TT_EN => 1
                    * 0 RFUI => 0
                    * 0
                    *   7  6  5  4  3  2  1  0
                    *   0  0  0  0  0  0  1  0          => 0x02
            */
            //byte[] page02_05 = tag.read(0x02);
            //byte[] nj = tag.readTTStatus();
            byte[] ttConfiguration = new byte[]{
                    cfgPages[0], (byte) 0x00, (byte) cfgPages[2], (byte) cfgPages[3],
                    (byte) (cfgPages[4] | 0x10), cfgPages[5], cfgPages[6], cfgPages[7]
            };
            //byte[] dynamicLockPage = tag.read(0x28);
            //tag.writeConfigBytes(ttConfiguration);
            //byte[] pom = tag.read(0x2D);
            //pom = openState;

            tag.writeCustomTTMessage(openState);

            // lock TT message - Once is locked, you cannot change it.
            //ttConfiguration[1] = 0x02;
            tag.writeConfigBytes(ttConfiguration);
        } catch (Exception ex) {
            // TODO: 25.01.2018 Throw exception
        }
    }

    private String getTTState() {
        setSV(tag.getUID());
        byte[] bytes = calculateCMAC(SV2);
        if (bytes == null) {
            return "00000000";
        }
        byte[] closeState = new byte[]{
                bytes[9], bytes[11], bytes[13], bytes[15]
        };
        openState = new byte[]{
                bytes[1], bytes[3], bytes[5], bytes[7]
        };
        return Utilities.byteToHexString(closeState);
    }

    private String getUID() {
        return Utilities.dumpBytes(tag.getUID()).split("x", 2)[1];
    }

    private void setSV(byte[] UID) {
        Utilities.dumpBytes(UID);
        SV2[6] = UID[0];
        SV2[7] = UID[1];
        SV2[8] = UID[2];
        SV2[9] = UID[3];
        SV2[10] = UID[4];
        SV2[11] = UID[5];
        SV2[12] = UID[6];
    }

    private byte[] calculateCMAC(byte[] SV2) {
        try {
            Crypto crypto = new Crypto(new com.nxp.nfclib.defaultimpl.Utilities());
            ICryptoGram iCryptoGram = crypto.getAESCrypto(ICrypto.CryptAlgoMode.ECB);
            KeyData keyData = new KeyData();
            Key key = new SecretKeySpec(DEFAULT_KEY_AES, "AES");
            keyData.setKey(key);
            return iCryptoGram.getCMAC(keyData, SV2);
        } catch (Exception ex) {
            return null;
        }
    }
}
