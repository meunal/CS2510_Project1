import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class PeerServer extends Server {
    private String indexIP;
    private int indexPort;

    public PeerServer(int _port, String _serverID, String _indexIP, int _indexPort) {
        super(_port, _serverID);
        indexIP = _indexIP;
        indexPort = _indexPort;
    }

    public void start() {
        try {
            register();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test() throws Exception {
        Socket sock = new Socket(indexIP, indexPort);

        ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

        Message req = new Message("LOOKUP");
        req.addContent("document.pdf");
        oos.writeObject(req);
        Message resp = (Message) ois.readObject();
        FileSources s = (FileSources) resp.getContent().get(0);
        System.out.println(s.getName() + ", " + s.getSize() + ", " + s.getSourceList().toString());

        ois.close();
        oos.close();
    }

    private boolean register() {
        File folder = new File("./" + serverID);

        try {
            if (folder.exists() && folder.isDirectory() && folder.canRead() && folder.canWrite()) {
                Socket sock = new Socket(indexIP, indexPort);

                ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

                String source = InetAddress.getLocalHost().getHostAddress().trim() + ":" + port;
                ArrayList<FileMeta> files = new ArrayList<FileMeta>();

                for (final File f : folder.listFiles()) {
                    System.out.println(f.getName());
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

                return true;
            } else {
                throw new IOException("Directory error! Directory " + serverID + " does not exists or you do not have read/write permission.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public String getIndexIP() {
        return indexIP;
    }

    public int getIndexPort() {
        return indexPort;
    }
}
