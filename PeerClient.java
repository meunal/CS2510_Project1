import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class PeerClient extends Thread {
    Peer master;
    FileSources fs;

    public PeerClient(Peer _master) {
        master = _master;
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Welcome to the peer server client");
        int selection = 1;

        while(selection != 0) {
            System.out.println("Please make a selection");
            if (fs == null) {
                System.out.println("1) Lookup for a file");
            } else {
                System.out.println("1) Lookup for a new file");
                System.out.println("2) Download last looked file " + fs.getName());
            }

            switch (sc.nextInt()) {
                case 1:
                    fs = null;
                    System.out.print("Enter filename: ");
                    lookup(sc.next());
                    break;
                case 2:
                    download();
                    break;
                default:
                    selection = 0;
                    break;
            }
        }
    }

    private void lookup(String _file) {
        try {
            Socket sock = new Socket(master.getIndexIP(), master.getIndexPort());
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

            long start = System.currentTimeMillis();

            Message req = new Message("LOOKUP");
            req.addContent(_file);
            oos.writeObject(req);
            Message response = (Message) ois.readObject();

            long end = System.currentTimeMillis();
            master.addLookupTime(end - start);

            if (response.getMessage().equals("SUCCESS")) {
                fs = (FileSources) response.getContent().get(0);
                System.out.println(_file + " is found on " + fs.getSourceList().size() + " machines.");
                System.out.println(fs.getSourceList().toString());
            } else {
                System.out.println(response.getMessage() + ": " + response.getContent().get(0));
            }

            oos.close();
            ois.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void download() {
        // download logic here based on how many source in the sourcelist.
        // if there is one, means we are downloading from single source
        // otherwise we need to do chunk downloading
    }

    public String getServerID() {
        return master.getServerID();
    }
}
