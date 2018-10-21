package taplinx.nxp.com.hackathontest;

/**
 * Created by nxf41757 on 17.10.2018.
 */

public interface DesfireInterface {

    boolean isConnected();

    void connect();

    void disconnect();

    void authenticate();

    int[] getApplicationIDs();

    byte[] getFilesIDs();

    int[] getISOFilesIDs();

    void createFile();

    void createApplication();
}
