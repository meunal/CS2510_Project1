import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class is to perform a lookup call to Indexing Server in test mode
 * @author Mesut Erhan Unal
 */
public class TestClient extends Thread {
    Test t;
    String job;
    int id;

    /**
     * Default constructor
     * @param _t: Test object which instantiated this thread
     * @param _job: File name which will be looked up on Indexing Server
     * @param _id: Number of this job
     */
    public TestClient(Test _t, String _job, int _id) {
        t = _t;
        job = _job;
        id = _id;
    }

    /**
     * Overridden run method to perform lookup call
     */
    @Override
    public void run() {
        try {
            // Open up a socket and streams to communicate with the Indexing Server
            Socket sock = new Socket(Utils.getIP(), 13001);
            final ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            final ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

            // Measure lookup time
            long start = System.currentTimeMillis();
            // Prepare a Message object and send lookup request
            Message req = new Message("LOOKUP");
            req.addContent(job);
            oos.writeObject(req);

            // Get response
            Message response = (Message) ois.readObject();
            // Measure current time as end and calculate the size of (request + response)
            long end = System.currentTimeMillis();
            long totalBytes = Utils.calculateBytes(req);
            totalBytes += Utils.calculateBytes(response);

            // Add lookup time and total bytes into corresponding lists
            t.addLookupTimes(end - start);
            t.addTotalBytes(totalBytes);

            // Close streams and socket
            oos.close();
            ois.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            t.increaseCompleted();
        }
    }
}
