package taplinx.nxp.com.hackathontest;

/**
 * Created by nxf41757 on 16.10.2018.
 */

public interface NTAGInterface {

    void connect();

    void disconnect();

    boolean isConnected();

    void writeNDEF();
}
