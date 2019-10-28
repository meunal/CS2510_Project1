import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is to create an Indexing Server. It listens a specific port for incoming connections
 * and creates new thread (IndexingServerThread) for each one of them. It also keeps lookup table
 * for files and lookup times recorded by each thread.
 * @author Mesut Erhan Unal
 */
public class IndexingServer extends Server {
    private ConcurrentHashMap<String, FileSources> lookup;
    private List<Long> responseTimes;
    private Random rand;
    private boolean test;

    /**
     * Default constructor
     * @param _port: Port number which to be listened by Indexing Server
     * @param _serverID: Unique id for the Indexing Server
     * @param _type: Type of the system, SINGLE or CHUNK
     * @param _test: Boolean flag to specify either we are running on test mode
     */
    public IndexingServer(int _port, String _serverID, SystemType _type, boolean _test) {
        super(_port, _serverID, _type);
        lookup = new ConcurrentHashMap<String, FileSources>();
        responseTimes = Collections.synchronizedList(new ArrayList<Long>());
        rand = new Random();
        test = _test;
    }

    /**
     * Starts the Indexing Server
     */
    public void start() {
        try {
            // If we are running on test mode, fill the lookup table (ConcurrentHashMap) with arbitrary data
            if (test) fillHashMap();
            // Add a ShutDownHook to perform statistics calculation at the end
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new ServerShutDownListener(this));

            // Open up a serversocket
            final ServerSocket serverSock = new ServerSocket(port);
            System.out.println(String.format("Indexing server is running on %s:%d", Utils.getIP(), port));

            Socket sock = null;
            IndexingServerThread t = null;

            // Listen to incoming connections
            while(true) {
                sock = serverSock.accept();
                t = new IndexingServerThread(sock, this);
                t.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add file into lookup table (ConcurrentHashMap)
     * @param _meta: FileMeta object of the file
     * @param _source: Source of the file (e.g. 192.168.1.12:2460)
     */
    public void addFile(FileMeta _meta, String _source) {
        try {
            // Get filename and file size from FileMeta object
            String name = _meta.getName();
            long size = _meta.getSize();
            // If filename is not present in the lookup table, add first with
            // a new FileSource object as a key and empty sourcelist
            lookup.putIfAbsent(name, new FileSources(name, size));
            FileSources temp = lookup.get(name);
            // Add source into sourcelist for this file
            synchronized (temp) {
                temp.addToSourceList(_source);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds up a FileSource object with source list for given filename
     * @param _filename: Requested filename
     * @return FileSource object for requested file
     */
    public FileSources getFileSources(String _filename) {
        // Get FileSource object from ConcurrentHashMap for given filename
        FileSources temp = lookup.get(_filename);

        // If there is no key for the filename, return null
        if (temp == null) return null;

        // Create a new FileSource object for response
        FileSources resp = new FileSources(temp.getName(), temp.getSize());
        ArrayList<String> sources = new ArrayList<String>();

        // Generate a random number bounded by size of the source list
        int r = rand.nextInt(temp.getSourceList().size());

        if (type == SystemType.SINGLE) {
            // If system type is SINGLE, then add a random source
            resp.addToSourceList(temp.getSourceList().get(r));
        } else {
            // Else, add all sources
            for (String s : temp.getSourceList())
                resp.addToSourceList(s);
        }

        return resp;
    }

    /**
     * This method fills lookup table (ConcurrentHashMap) with virtual data
     * created by Test.java.
     */
    public void fillHashMap() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("test_files.txt"));
            String temp;
            while((temp = br.readLine()) != null) {
                String [] line = temp.split(",");
                int sourcesize = line.length - 2;

                FileSources fs = new FileSources(line[0], Integer.parseInt(line[1]));
                for (int i = 0; i < sourcesize; i++)
                    fs.addToSourceList(line[2 + i]);
                lookup.put(line[0], fs);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add given response time into responseTimes list
     * @param _rt: Response time
     */
    public void addResponseTime(long _rt) {
        responseTimes.add(_rt);
    }

    /**
     * Getter for responseTimes list
     * @return responseTimes list
     */
    public List<Long> getResponseTimes() {
        return responseTimes;
    }
}

/**
 * This class is a shutdownlistener thread for IndexingServer
 * @author Mesut Erhan Unal
 */
class ServerShutDownListener extends Thread {
    public IndexingServer server;

    public ServerShutDownListener (IndexingServer _server) {
        server = _server;
    }

    /**
     * Overridden run function which will be invoked when we shut down IndexingServer
     * Calculates statistics and prints them out. Also writes them into Logs/IndexingServer.log.
     */
    @Override
    public void run() {
        System.out.println("Shutting down the IndexingServer\n");
        int totalRequests = server.getResponseTimes().size();
        long totalTime = 0;
        for (long t : server.getResponseTimes())
            totalTime += t;
        System.out.println("Statistics:");
        System.out.println(String.format("Total Requests: %d", totalRequests));
        System.out.println(String.format("Average Response Time: %.4f", (totalTime * 1.0) / totalRequests));

        try {
            BufferedWriter bf = new BufferedWriter(new FileWriter("./Logs/IndexingServer.log"));
            bf.write(String.format("Total Requests: %d\n", totalRequests));
            bf.write(String.format("Average Response Time: %.4f\n", (totalTime * 1.0) / totalRequests));
            bf.close();
        } catch (Exception e) {
            System.out.println("Could not write statistics into Logs/IndexingServer.log");
            e.printStackTrace();
        }
    }
}
