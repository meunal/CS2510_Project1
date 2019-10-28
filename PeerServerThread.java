import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This class is to serve for a download request
 * @author Mesut Erhan Unal
 */
public class PeerServerThread extends Thread {
    private Socket sock;
    private PeerServer server;

    /**
     * Default constructor
     * @param _sock Socket object created for the connection
     * @param _server PeerServer object instantiated this thread
     */
    public PeerServerThread(Socket _sock, PeerServer _server) {
        sock = _sock;
        server = _server;
    }

    /**
     * Overridden run method to respond download request
     */
    @Override
    public void run() {
        try {
            // Open up streams
            final ObjectInputStream input = new ObjectInputStream(sock.getInputStream());
            final ObjectOutputStream output = new ObjectOutputStream(sock.getOutputStream());

            // Read request
            Message request = (Message) input.readObject();
            Message response = null;

            if (request.getMessage().equals("DOWNLOAD_CHUNK")) { // Client wants a chunk
                // Extract FileMeta, chunk size and which chunk to be sent from the request
                FileMeta fm = (FileMeta) request.getContent().get(0);
                int chunkSize = (int) request.getContent().get(1);
                int whichChunk = (int) request.getContent().get(2);

                // Get my ID to read the file from the directory
                String myID = server.getServerID();
                File requestedFile = new File(String.format("./%s/%s", myID, fm.getName()));

                // Check if requested file exists in our directory and we have a read permission
                if (requestedFile.exists() && requestedFile.isFile() && requestedFile.canRead()) {
                    // Create a new Message object for response
                    response = new Message("SUCCESS");
                    // Calculate real chunk size. If we are requested the last chunk,
                    // chunk size might be different.
                    int realBufferSize = chunkSize;
                    int chunksInFile = (int) Math.ceil(fm.getSize() * 1.0 / chunkSize);
                    if (chunksInFile == whichChunk) {
                        realBufferSize = (int) fm.getSize() - ((whichChunk - 1) * chunkSize);
                    }

                    // Create a byte array buffer for the chunk
                    byte [] buffer = new byte[realBufferSize];
                    FileInputStream fis = new FileInputStream(requestedFile);
                    // Skip previous chunks
                    fis.skip((whichChunk - 1) * chunkSize);
                    // Read requested chunk into buffer
                    fis.read(buffer);
                    fis.close();
                    // Add buffer into response
                    response.addContent(buffer);
                } else {
                    response = new Message("FAIL");
                    response.addContent("File does not exist or cannot read");
                }

                // Send response
                output.writeObject(response);
            } else if (request.getMessage().equals("DOWNLOAD")) { // Client wants full file download
                // Extract FileMeta object from the request
                FileMeta fm = (FileMeta) request.getContent().get(0);

                // Get this Peer's id to read the file from its directory
                String myID = server.getServerID();
                File requestedFile = new File(String.format("./%s/%s", myID, fm.getName()));

                // Check if file exists and we have read permission
                if (requestedFile.exists() && requestedFile.isFile() && requestedFile.canRead()) {
                    // Create a new Message object for response
                    response = new Message("SUCCESS");
                    // Create a byte array buffer for the read
                    // and read requested file into it
                    byte [] buffer = new byte[(int) fm.getSize()];
                    FileInputStream fis = new FileInputStream(requestedFile);
                    // Read requested file into buffer
                    fis.read(buffer);
                    fis.close();
                    // Add buffer into response
                    response.addContent(buffer);
                } else {
                    response = new Message("FAIL");
                    response.addContent("File does not exist or cannot read");
                }

                // Send response
                output.writeObject(response);
            } else { // Request is not DOWNLOAD and DOWNLOAD_CHUNK. Bad request.
                response = new Message("FAIL");
                response.addContent("Bad request");
                output.writeObject(response);
                throw new IllegalArgumentException("BAD REQUEST");
            }

            // Close streams and socket
            input.close();
            output.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
