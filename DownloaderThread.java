import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class DownloaderThread extends Thread {
    private String serverID;
    private String peerAddr;
    private int chunkNumber;
    private int chunkSize;
    private FileMeta fm;

    public DownloaderThread(String _serverID, String _peerAddr, int _chunkNumber, int _chunkSize, FileMeta _fm) {
        serverID = _serverID;
        peerAddr = _peerAddr;
        chunkNumber = _chunkNumber;
        chunkSize = _chunkSize;
        fm = _fm;
    }

    @Override
    public void run() {
        try {
            String [] addr = peerAddr.split(":");
            Socket sock = new Socket(addr[0], Integer.parseInt(addr[1]));
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

            Message req = new Message("DOWNLOAD_CHUNK");
            req.addContent(fm);
            req.addContent(chunkSize);
            req.addContent(chunkNumber);
            oos.writeObject(req);

            Message response = (Message) ois.readObject();
            FileOutputStream fos = new FileOutputStream("./" + serverID + "/" + fm.getName() + "-chunk" + chunkNumber);
            fos.write((byte []) response.getContent().get(0));
            fos.close();

            oos.close();
            ois.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
