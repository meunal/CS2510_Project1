import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * This thread is to serve a request comes into IndexingServer
 * @author Mesut Erhan Unal
 */
public class IndexingServerThread extends Thread {
    private IndexingServer server;
    private Socket sock;

    /**
     * Default constructor
     * @param _sock: Socket object created by IndexingServer for the new connection
     * @param _server: IndexingServer object
     */
    public IndexingServerThread(Socket _sock, IndexingServer _server) {
        this.sock = _sock;
        this.server = _server;
    }

    /**
     * Overridden run method
     * Identifies different request types and acts accordingly
     */
    @Override
    public void run() {
        try {
            // Open up streams
            final ObjectInputStream input = new ObjectInputStream(sock.getInputStream());
            final ObjectOutputStream output = new ObjectOutputStream(sock.getOutputStream());

            // Read the Message object sent by a client
            Message request = (Message) input.readObject();
            Message response = null;

            if (request.getMessage().equals("LOOKUP")) { // Client wants to lookup
                // Measure lookup time by keeping a start
                long start = System.currentTimeMillis();

                // Perform search on ConcurrentHashMap object
                FileSources sources = server.getFileSources((String) request.getContent().get(0));

                if (sources == null) { // If file not found, respond with a fail message
                    response = new Message("FAIL");
                    response.addContent("No file found with this name in the IndexingServer");
                } else { // If found return success and a source or complete source list based on the system type
                    response = new Message("SUCCESS");
                    response.addContent(sources);
                }

                // Send response
                output.writeObject(response);
                // Measure current time as ending time
                long end = System.currentTimeMillis();
                // Save start - end as response time
                server.addResponseTime(end - start);
            } else if (request.getMessage().equals("REGISTER")) { // Client wants to register
                System.out.println("GOT REGISTER");
                // Get source address and file list (as FileMeta objects)
                ArrayList<Object> content = request.getContent();
                String source = (String) content.get(0);
                ArrayList<FileMeta> files = (ArrayList<FileMeta>) content.get(1);

                // For each file add file to the server
                for (FileMeta f : files)
                    server.addFile(f, source);

                // Send the response
                response = new Message("SUCCESS");
                output.writeObject(response);
            } else { // If Message header is not LOOKUP and REGISTER, then it is bad request
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
