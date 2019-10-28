import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Downloader class for multithreaded download
 * @author Mesut Erhan Unal
 */
public class DownloaderThread extends Thread {
    private String serverID;
    private String peerAddr;
    private int chunkNumber;
    private int chunkSize;
    private FileMeta fm;

    /**
     * Default constructor
     * @param _serverID: ServerID of the peer that instantiated the download (need this to know which directory to write)
     * @param _peerAddr: Address to connect to download the chunk
     * @param _chunkNumber: Which chunk we are requesting
     * @param _chunkSize: Size of the chunk in bytes
     * @param _fm: FileMeta object of the file
     */
    public DownloaderThread(String _serverID, String _peerAddr, int _chunkNumber, int _chunkSize, FileMeta _fm) {
        serverID = _serverID;
        peerAddr = _peerAddr;
        chunkNumber = _chunkNumber;
        chunkSize = _chunkSize;
        fm = _fm;
    }

    /**
     * Overridden run method of the thread
     * Downloads a chunk from specified source and writes it to the disk
     */
    @Override
    public void run() {
        try {
            // Get IP and port from the address string
            String [] addr = peerAddr.split(":");

            // Start the connection and open streams
            Socket sock = new Socket(addr[0], Integer.parseInt(addr[1]));
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

            // Build up Message object to be sent
            Message req = new Message("DOWNLOAD_CHUNK");
            req.addContent(fm);
            req.addContent(chunkSize);
            req.addContent(chunkNumber);
            // Sent Message object
            oos.writeObject(req);

            // Get the downloaded chunk
            Message response = (Message) ois.readObject();
            // Write this chunk into this peer's directory as filename.extension-chunkN
            FileOutputStream fos = new FileOutputStream("./" + serverID + "/" + fm.getName() + "-chunk" + chunkNumber);
            fos.write((byte []) response.getContent().get(0));
            fos.close();

            // Close everything and die
            oos.close();
            ois.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
