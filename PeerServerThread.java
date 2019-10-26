import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PeerServerThread extends Thread {
    private Socket sock;
    private PeerServer server;

    public PeerServerThread(Socket _sock, PeerServer _server) {
        sock = _sock;
        server = _server;
    }

    @Override
    public void run() {
        try {
            final ObjectInputStream input = new ObjectInputStream(sock.getInputStream());
            final ObjectOutputStream output = new ObjectOutputStream(sock.getOutputStream());

            Message request = (Message) input.readObject();
            Message response = null;

            if (request.getMessage().equals("DOWNLOAD_CHUNK")) {
                FileMeta fm = (FileMeta) request.getContent().get(0);
                int chunkSize = (int) request.getContent().get(1);
                int whichChunk = (int) request.getContent().get(2);

                String myID = server.getServerID();
                File requestedFile = new File(String.format("./%s/%s", myID, fm.getName()));

                if (requestedFile.exists() && requestedFile.isFile() && requestedFile.canRead()) {
                    response = new Message("SUCCESS");
                    int realBufferSize = chunkSize;
                    int chunksInFile = (int) Math.ceil(fm.getSize() * 1.0 / chunkSize);
                    if (chunksInFile == whichChunk) {
                        realBufferSize = (int) fm.getSize() - ((whichChunk - 1) * chunkSize);
                    }
                    byte [] buffer = new byte[realBufferSize];
                    FileInputStream fis = new FileInputStream(requestedFile);
                    fis.skip((whichChunk - 1) * chunkSize);
                    fis.read(buffer);
                    fis.close();
                    response.addContent(buffer);
                    // response.addContent(whichChunk);
                } else {
                    response = new Message("FAIL");
                    response.addContent("File does not exist or cannot read");
                }

                output.writeObject(response);
            } else if (request.getMessage().equals("DOWNLOAD")) {
                FileMeta fm = (FileMeta) request.getContent().get(0);
                String myID = server.getServerID();
                File requestedFile = new File(String.format("./%s/%s", myID, fm.getName()));

                if (requestedFile.exists() && requestedFile.isFile() && requestedFile.canRead()) {
                    response = new Message("SUCCESS");
                    byte [] buffer = new byte[(int) fm.getSize()];
                    FileInputStream fis = new FileInputStream(requestedFile);
                    fis.read(buffer);
                    fis.close();
                    response.addContent(buffer);
                } else {
                    response = new Message("FAIL");
                    response.addContent("File does not exist or cannot read");
                }

                output.writeObject(response);
            } else {
                response = new Message("FAIL");
                response.addContent("Bad request");
                output.writeObject(response);
                throw new IllegalArgumentException("BAD REQUEST");
            }

            input.close();
            output.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
