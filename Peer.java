import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer extends Server {
    private String indexIP;
    private int indexPort;
    private List<Long> lookupTimes;
    private List<Long> downloadTimes;

    public Peer(int _port, String _serverID, String _indexIP, int _indexPort) {
        super(_port, _serverID);
        indexIP = _indexIP;
        indexPort = _indexPort;
        lookupTimes = Collections.synchronizedList(new ArrayList<Long>());
        downloadTimes = Collections.synchronizedList(new ArrayList<Long>());
    }

    public void start() {
        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new PeerShutDownListener(this));
            register();
            final ExecutorService pool = Executors.newFixedThreadPool(2);
            PeerServer ps = new PeerServer(this);
            PeerClient pc = new PeerClient(this);
            pool.submit(ps);
            pool.submit(pc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void register() {
        File folder = new File("./" + serverID);

        try {
            if (folder.exists() && folder.isDirectory() && folder.canRead() && folder.canWrite()) {
                Socket sock = new Socket(indexIP, indexPort);

                ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

                String source = InetAddress.getLocalHost().getHostAddress().trim() + ":" + port;
                ArrayList<FileMeta> files = new ArrayList<FileMeta>();

                for (final File f : folder.listFiles()) {
                    System.out.println(f.getName() + ", " + f.length());
                    files.add(new FileMeta(f.getName(), f.length()));
                }

                Message req = new Message("REGISTER");
                req.addContent(source);
                req.addContent(files);
                oos.writeObject(req);

                Message resp = (Message) ois.readObject();
                if (resp.getMessage().equals("SUCCESS")) {
                    System.out.println("Successfully registered with the IndexingServer");
                }

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

    public String getIndexIP() {
        return indexIP;
    }

    public int getIndexPort() {
        return indexPort;
    }

    public void addLookupTime(long _lt) {
        lookupTimes.add(_lt);
    }

    public void addDownloadTime(long _dt) {
        downloadTimes.add(_dt);
    }

    public List<Long> getLookupTimes() {
        return lookupTimes;
    }

    public List<Long> getDownloadTimes() {
        return downloadTimes;
    }
}

class PeerShutDownListener extends Thread {
    public Peer master;

    public PeerShutDownListener (Peer _master) {
        master = _master;
    }

    public void run()
    {
        System.out.println("Shutting down the peer\n");


        // TO-DO: Write stats
    }
}