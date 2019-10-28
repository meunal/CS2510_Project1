import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class is to serve files as a Peer
 * It listens the given port and create a new PeerServerThread
 * for each incoming connection
 * @author Mesut Erhan Unal
 */
public class PeerServer extends Thread {
    Peer master;

    /**
     * Default constructor
     * @param _master: Peer object which instantiated this PeerServer
     */
    public PeerServer(Peer _master) {
        master = _master;
    }

    /**
     * Overridden run method which listens a port and creates new threads
     * for incoming connections
     */
    @Override
    public void run() {
        try {
            // Create a server socket and start listening to the specified port
            final ServerSocket serverSock = new ServerSocket(master.getPort());

            Socket sock = null;
            PeerServerThread t = null;

            // Listen to incoming connections and create a new
            // PeerServerThread for each one of them
            while(true) {
                sock = serverSock.accept();
                t = new PeerServerThread(sock, this);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Proxy for master.getServerID()
     * @return Peer's ServerID
     */
    public String getServerID() {
        return master.getServerID();
    }
}
