import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * This class is to perform lookup and download operations for a Peer
 * @author Mesut Erhan Unal
 */
public class PeerClient extends Thread {
    Peer master;
    FileSources fs;
    final int MAX_PARALLEL_JOBS = 5;

    /**
     * Default constructor
     * @param _master: Peer object which instantiated this PeerClient
     */
    public PeerClient(Peer _master) {
        master = _master;
    }

    /**
     * Overridden run method to interact with the users
     * through command-line interface
     */
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

    /**
     * Performs a lookup call to the Indexing Server
     * @param _file: File name for the lookup call
     */
    private void lookup(String _file) {
        try {
            // Open socket and streams to communicate with the Indexing Server
            Socket sock = new Socket(master.getIndexIP(), master.getIndexPort());
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

            // Measure lookup time
            long start = System.currentTimeMillis();

            // Build a Message object and send
            Message req = new Message("LOOKUP");
            req.addContent(_file);
            oos.writeObject(req);

            // Read response from Indexing Server
            Message response = (Message) ois.readObject();

            // Measure current time as end, and add end-start into Peer's lookupTimes list
            long end = System.currentTimeMillis();
            master.addLookupTime(end - start);

            if (response.getMessage().equals("SUCCESS")) { // We got a good response. File was found.
                // Assign response to the fs attribute
                fs = (FileSources) response.getContent().get(0);
                System.out.println(_file + " is found on " + fs.getSourceList().size() + " machines.");
                System.out.println(fs.getSourceList().toString());
            } else { // Failure
                System.out.println(response.getMessage() + ": " + response.getContent().get(0));
            }

            // Close streams and socket
            oos.close();
            ois.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads the last looked file
     */
    private void download() {
        try {
            // Make sure no duplicate sources in the source list by making it a set
            Set<String> sources = new HashSet<String>(fs.getSourceList());

            // Check if this Peer already has this file
            if (sources.contains(Utils.getIP() + ":" + master.getPort())) {
                fs = null;
                System.out.println("You already have this file");
                return;
            }

            // Change source list type from set to list again
            List<String> sourceList = new ArrayList<String>(sources);


            int num_jobs = MAX_PARALLEL_JOBS;
            // If source list is less than MAX_PARALLEL_JOBS
            // then we need to create sourceList.size() threads
            if (sourceList.size() < num_jobs)
                num_jobs = sourceList.size();

            // If source list had only one source
            // Perform single download
            if (num_jobs == 1) {
                // Extract IP and port of the Peer to be connected to
                String [] addr = sourceList.get(0).split(":");

                // Measure download time
                long start = System.currentTimeMillis();

                // Open socket and streams
                Socket sock = new Socket(addr[0], Integer.parseInt(addr[1]));
                final ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
                final ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

                // Request the file as a whole
                Message req = new Message("DOWNLOAD");
                req.addContent(new FileMeta(fs.getName(), fs.getSize()));
                oos.writeObject(req);

                // Get the response
                Message response = (Message) ois.readObject();
                // Measure current time as end, and add end-start into Peer's downloadTimes list
                long end = System.currentTimeMillis();
                master.addDownloadTime(end - start);

                // Write file into disk
                FileOutputStream fos = new FileOutputStream("./" + master.getServerID() + "/" + fs.getName());
                fos.write((byte []) response.getContent().get(0));
                fos.close();

                // Close streams and socket
                oos.close();
                ois.close();
                sock.close();
            } else { // If we have multiple sources
                // Calculate the chunk size
                int chunkSize = (int) Math.ceil(fs.getSize() * 1.0 / num_jobs);
                // Create a List of DownloaderThreads
                ArrayList<DownloaderThread> dlThreadList = new ArrayList<DownloaderThread>();

                // For each source create a new DownloaderThread
                for (int i = 0; i < num_jobs; i++)
                    dlThreadList.add(new DownloaderThread(master.getServerID(), sourceList.get(i), i + 1, chunkSize, new FileMeta(fs.getName(), fs.getSize())));

                // Measure download time
                long start = System.currentTimeMillis();

                // Start threads and block execution until all of them are done
                for (int i = 0; i < num_jobs; i++)
                    dlThreadList.get(i).start();
                for (int i = 0; i < num_jobs; i++)
                    dlThreadList.get(i).join();

                // Measure current time as end and add end-start into Peer's downloadTimes list
                long end = System.currentTimeMillis();
                master.addDownloadTime(end - start);

                // Merge the chunks
                // Create a new file with the file's original name
                FileOutputStream fos = new FileOutputStream("./" + master.getServerID() + "/" + fs.getName(), true);

                // Read each chunk and append them into newly created file
                for (int i = 0; i < num_jobs; i++) {
                    File chunk = new File("./" + master.getServerID() + "/" + fs.getName() + "-chunk" + (i + 1));
                    byte [] temp = new byte[(int) chunk.length()];
                    FileInputStream fis = new FileInputStream(chunk);
                    fis.read(temp);
                    fos.write(temp);
                    fis.close();
                    chunk.delete();
                }

                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        fs = null;
    }
}
