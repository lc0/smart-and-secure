package taplinx.nxp.com.hackathontest.Tags;

import com.nxp.nfclib.KeyType;
import com.nxp.nfclib.defaultimpl.KeyData;
import com.nxp.nfclib.desfire.IDESFireEV1;
import com.nxp.nfclib.interfaces.IKeyData;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import taplinx.nxp.com.hackathontest.DesfireInterface;

/**
 * Created by nxf41757 on 17.10.2018.
 */

public class Desfire implements DesfireInterface {

    IDESFireEV1 tag;

    public final byte[] DEFAULT_KEY_AES =
            {                                                 // Default AES128 key
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
            };

    public Desfire(IDESFireEV1 tag) {
        this.tag = tag;
    }

    @Override
    public void connect() {
        if (!isConnected()) {
            tag.getReader().connect();
            tag.getReader().setTimeout(2000);
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
    public void authenticate() {
        try {
            KeyData keyData = new KeyData();
            Key key = new SecretKeySpec(DEFAULT_KEY_AES, "DESede");
            keyData.setKey(key);
            tag.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.TWO_KEY_THREEDES, keyData);
        } catch (Exception ex) {
            String m = ex.getMessage();
        }
    }

    @Override
    public int[] getApplicationIDs() {
        // Application 0 must be selected to get application IDs
        tag.selectApplication(0);
        return tag.getApplicationIDs();
    }

    @Override
    public byte[] getFilesIDs() {
        return tag.getFileIDs();
    }

    @Override
    public int[] getISOFilesIDs() {
        return tag.getISOFileIDs();
    }

    @Override
    public void createFile() {

    }

    @Override
    public void createApplication() {

    }

    public String getTagName() {
        return tag.getType().getTagName();
    }

    public String getAuthStatus() {
        return tag.getAuthStatus();
    }
}
