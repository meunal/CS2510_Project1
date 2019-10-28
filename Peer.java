import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is to create a Peer. It keeps lookup times and download times measured by this client.
 * It spawns two threads: PeerClient and PeerServer.
 * @author Mesut Erhan Unal
 */
public class Peer extends Server {
    private String indexIP;
    private int indexPort;
    private List<Long> lookupTimes;
    private List<Long> downloadTimes;

    /**
     * Default constructor
     * @param _port: Port number to be listened by this peer
     * @param _serverID: A unique ID to distinguish this peer from other. There needs to be a directory with the same name
     * @param _indexIP: IndexingServer's IP address
     * @param _indexPort: IndexingServer's port number
     */
    public Peer(int _port, String _serverID, String _indexIP, int _indexPort) {
        super(_port, _serverID);
        indexIP = _indexIP;
        indexPort = _indexPort;
        lookupTimes = Collections.synchronizedList(new ArrayList<Long>());
        downloadTimes = Collections.synchronizedList(new ArrayList<Long>());
    }

    /**
     * Starts a new Peer
     */
    public void start() {
        try {
            // Add a ShutDownHook to perform calculations at the end
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new PeerShutDownListener(this));

            // Call register method to register with the IndexingServer
            register();

            // Create an ExecutorService for PeerServer and PeerClient threads
            final ExecutorService pool = Executors.newFixedThreadPool(2);

            // Create PeerServer and PeerClient threads and run them
            PeerServer ps = new PeerServer(this);
            PeerClient pc = new PeerClient(this);
            pool.submit(ps);
            pool.submit(pc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method register this Peer with the Indexing Server
     */
    private void register() {
        // Create a file object for this Peer's directory
        File folder = new File("./" + serverID);

        try {
            // If folder exists and is a dir and we have read/write permisssion
            if (folder.exists() && folder.isDirectory() && folder.canRead() && folder.canWrite()) {
                // Open a socket to communicate with the Indexing Server
                Socket sock = new Socket(indexIP, indexPort);

                // Open streams
                ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

                // Calculate the address of this peer
                String source = Utils.getIP() + ":" + port;
                ArrayList<FileMeta> files = new ArrayList<FileMeta>();

                // Do not include hidden files or system files
                File [] fileList = folder.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return !file.isHidden();
                    }
                });

                // For each file in the directory, calculate its size and create
                // a FileMeta object.
                for (final File f : fileList) {
                    System.out.println(f.getName() + ", " + f.length());
                    files.add(new FileMeta(f.getName(), f.length()));
                }

                // Build a Message object to send to Indexing Server for register
                Message req = new Message("REGISTER");
                // Add own address and file list into Message object then send
                req.addContent(source);
                req.addContent(files);
                oos.writeObject(req);

                // Get the response from the Indexing Server
                Message resp = (Message) ois.readObject();
                if (resp.getMessage().equals("SUCCESS")) {
                    System.out.println("Successfully registered with the IndexingServer");
                }

                // Close streams and socket
                ois.close();
                oos.close();
                sock.close();
            } else {
                throw new IOException("Directory error! Directory " + serverID + " does not exists or you do not have read/write permission.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Getter for indexIP
     * @return Indexing Server's IP
     */
    public String getIndexIP() {
        return indexIP;
    }

    /**
     * Getter for indexPort
     * @return Indexing Server's port number
     */
    public int getIndexPort() {
        return indexPort;
    }

    /**
     * Adds lookup time into lookupTimes list
     * @param _lt: Lookup time to add
     */
    public void addLookupTime(long _lt) {
        lookupTimes.add(_lt);
    }

    /**
     * Adds download time into downloadTimes list
     * @param _dt: Download time to add
     */
    public void addDownloadTime(long _dt) {
        downloadTimes.add(_dt);
    }

    /**
     * Getter for lookupTimes
     * @return lookupTimes list
     */
    public List<Long> getLookupTimes() {
        return lookupTimes;
    }

    /**
     * Getter for downloadTimes
     * @return downloadTimes list
     */
    public List<Long> getDownloadTimes() {
        return downloadTimes;
    }
}

/**
 * This class is a shutdown listener thread for Peer object
 * @author Mesut Erhan Unal
 */
class PeerShutDownListener extends Thread {
    public Peer master;

    /**
     * Default constructor
     * @param _master: Peer object
     */
    public PeerShutDownListener (Peer _master) {
        master = _master;
    }

    /**
     * Overridden run method which will be run when Peer is shut down.
     * Does the statistics calculations and prints them out, also writes them in
     * Logs/Peer_[ID].log
     */
    @Override
    public void run() {
        System.out.println("Shutting down the peer\n");
        List<Long> lookupTimes = master.getLookupTimes();
        List<Long> downloadTimes = master.getDownloadTimes();

        long totalLookupTime = 0;
        long totalDownloadTime = 0;

        for (int i = 0; i < lookupTimes.size(); i++)
            totalLookupTime += lookupTimes.get(i);
        for (int i = 0; i < downloadTimes.size(); i++)
            totalDownloadTime += downloadTimes.get(i);

        System.out.println("Statistics:");
        System.out.println("Total lookups: " + lookupTimes.size());
        System.out.println(String.format("Average lookup time in ms: %.4f", totalLookupTime * 1.0 / lookupTimes.size()));
        System.out.println("Total downloads: " + downloadTimes.size());
        System.out.println(String.format("Average download time in ms: %.4f", totalDownloadTime * 1.0 / downloadTimes.size()));

        try {
            BufferedWriter bf = new BufferedWriter(new FileWriter("./Logs/Peer_" + master.getServerID() + ".log"));
            bf.write(String.format("Total lookups: %d\n", lookupTimes.size()));
            bf.write(String.format("Average lookup time in ms: %.4f\n", totalLookupTime * 1.0 / lookupTimes.size()));
            bf.write(String.format("Total downloads: %d\n", downloadTimes.size()));
            bf.write(String.format("Average download time in ms: %.4f\n", totalDownloadTime * 1.0 / downloadTimes.size()));
            bf.close();
        } catch (Exception e) {
            System.out.println("Could not write statistics into Logs directory");
            e.printStackTrace();
        }
    }
}
