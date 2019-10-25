import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class IndexingServerThread extends Thread {
    private IndexingServer server;
    private Socket sock;

    public IndexingServerThread(Socket _sock, IndexingServer _server) {
        this.sock = _sock;
        this.server = _server;
    }

    @Override
    public void run() {
        try {
            final ObjectInputStream input = new ObjectInputStream(sock.getInputStream());
            final ObjectOutputStream output = new ObjectOutputStream(sock.getOutputStream());

            Message request = (Message) input.readObject();

            if (request.getMessage().equals("LOOKUP")) {
                long start = System.currentTimeMillis();

                FileSources sources = server.getFileSources((String) request.getContent().get(0));
                Message response = null;

                if (sources == null) {
                    response = new Message("FAIL");
                } else {
                    response = new Message("SUCCESS");
                    response.addContent(sources);
                }

                output.writeObject(response);

                long end = System.currentTimeMillis();
                server.addResponseTime(end - start);
            } else if (request.getMessage().equals("REGISTER")) {
                System.out.println("GOT REGISTER");
                ArrayList<Object> content = request.getContent();
                String source = (String) content.get(0);
                ArrayList<FileMeta> files = (ArrayList<FileMeta>) content.get(1);

                for (FileMeta f : files)
                    server.addFile(f, source);
                output.writeObject(new Message("SUCCESS"));
            } else {
                output.writeObject(new Message("FAIL"));
                throw new IllegalArgumentException("BAD REQUEST");
            }

            input.close();
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
