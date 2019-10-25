import java.net.ServerSocket;
import java.net.Socket;

public class PeerServer extends Thread {
    Peer master;

    public PeerServer(Peer _master) {
        master = _master;
    }

    @Override
    public void run() {
        try {
            final ServerSocket serverSock = new ServerSocket(master.getPort());

            Socket sock = null;
            PeerServerThread t = null;

            while(true) {
                sock = serverSock.accept();
                t = new PeerServerThread(sock, this);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getServerID() {
        return master.getServerID();
    }
}
