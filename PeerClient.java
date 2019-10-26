import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerClient extends Thread {
    Peer master;
    FileSources fs;
    final int MAX_PARALLEL_JOBS = 5;

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
        try {
            Set<String> sources = new HashSet<String>(fs.getSourceList()); // making sure no duplicates

            if (sources.contains(InetAddress.getLocalHost().getHostAddress().trim() + ":" + master.getPort())) {
                System.out.println("Already have this file");
                return;
            }

            List<String> sourceList = new ArrayList<String>(sources);

            int num_jobs = MAX_PARALLEL_JOBS;
            if (sourceList.size() < num_jobs)
                num_jobs = sourceList.size();

            if (num_jobs == 1) {
                String [] addr = sourceList.get(0).split(":");

                long start = System.currentTimeMillis();

                Socket sock = new Socket(addr[0], Integer.parseInt(addr[1]));
                ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

                Message req = new Message("DOWNLOAD");
                req.addContent(new FileMeta(fs.getName(), fs.getSize()));
                oos.writeObject(req);

                Message response = (Message) ois.readObject();

                long end = System.currentTimeMillis();
                master.addDownloadTime(end - start);

                FileOutputStream fos = new FileOutputStream("./" + master.getServerID() + "/" + fs.getName());
                fos.write((byte []) response.getContent().get(0));
                fos.close();

                oos.close();
                ois.close();
                sock.close();
            } else {
                int chunkSize = (int) Math.ceil(fs.getSize() * 1.0 / num_jobs);
                ArrayList<DownloaderThread> dlThreadList = new ArrayList<DownloaderThread>();

                for (int i = 0; i < num_jobs; i++)
                    dlThreadList.add(new DownloaderThread(master.getServerID(), sourceList.get(i), i + 1, chunkSize, new FileMeta(fs.getName(), fs.getSize())));

                long start = System.currentTimeMillis();

                for (int i = 0; i < num_jobs; i++)
                    dlThreadList.get(i).run();
                for (int i = 0; i < num_jobs; i++)
                    dlThreadList.get(i).join();

                long end = System.currentTimeMillis();
                master.addDownloadTime(end - start);

                FileOutputStream fos = new FileOutputStream("./" + master.getServerID() + "/" + fs.getName(), true);

                for (int i = 0; i < num_jobs; i++) {
                    File chunk = new File("./" + master.getServerID() + "/" + fs.getName() + "-chunk" + (i + 1));
                    FileInputStream fis = new FileInputStream(chunk);
                    fis.transferTo(fos);
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
