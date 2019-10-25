import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class IndexingServer extends Server {
    private ConcurrentHashMap<String, FileSources> lookup;
    private List<Long> responseTimes;
    private Random rand;

    public IndexingServer(int _port, String _serverID, SystemType _type) {
        super(_port, _serverID, _type);
        lookup = new ConcurrentHashMap<String, FileSources>();
        responseTimes = Collections.synchronizedList(new ArrayList<Long>());
        rand = new Random();
    }

    public void start() {
        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new ShutDownListener(this));

            final ServerSocket serverSock = new ServerSocket(port);
            System.out.println(String.format("Indexing server is running on %s:%d", InetAddress.getLocalHost().getHostAddress().trim(), port));

            Socket sock = null;
            IndexingServerThread t = null;

            while(true) {
                sock = serverSock.accept();
                t = new IndexingServerThread(sock, this);
                t.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addFile(FileMeta _meta, String _source) {
        try {
            String name = _meta.getName();
            long size = _meta.getSize();

            lookup.putIfAbsent(name, new FileSources(name, size));
            FileSources temp = lookup.get(name);
            synchronized (temp) {
                temp.addToSourceList(_source);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileSources getFileSources(String _filename) {
        FileSources temp = lookup.get(_filename);

        if (temp == null) return null;

        FileSources resp = new FileSources(temp.getName(), temp.getSize());
        ArrayList<String> sources = new ArrayList<String>();

        int r = rand.nextInt(temp.getSourceList().size());

        if (type == SystemType.SINGLE) {
            resp.addToSourceList(temp.getSourceList().get(r));
        } else {
            for (String s : temp.getSourceList())
                resp.addToSourceList(s);
        }

        return resp;
    }

    public void addResponseTime(long _rt) {
        responseTimes.add(_rt);
    }

    public List<Long> getResponseTimes() {
        return responseTimes;
    }
}

class ShutDownListener extends Thread
{
    public IndexingServer server;

    public ShutDownListener (IndexingServer _server) {
        server = _server;
    }

    public void run()
    {
        System.out.println("Shutting down server\n");
        int totalRequests = server.getResponseTimes().size();
        long totalTime = 0;
        for (long t : server.getResponseTimes())
            totalTime += t;
        System.out.println("Statistics:");
        System.out.println(String.format("Total Requests: %d", totalRequests));
        System.out.println(String.format("Average Response Time: %.4f", (totalTime * 1.0) / totalRequests));

        // maybe write a file too.
    }
}
