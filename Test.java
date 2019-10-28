import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is to test systems with varying parameters
 * @author Mesut Erhan Unal
 */
public class Test {
    private int M;
    private int N;
    private int F;
    private String type;
    private Random rand;
    private AtomicInteger completed;
    private List<String> jobQueue;
    private List<Long> totalBytes;
    private List<Long> lookupTimes;
    private final int NUMBER_OF_CONCURRENT_THREADS = 100;

    /**
     * Entry point for the Test
     * @param args: M, N, F, and Type (in this order)
     */
    public static void main(String [] args) {
        if (args.length != 4) {
            System.out.println("Test expects 4 inputs");
            System.out.println("1) Number of files in the system");
            System.out.println("2) Number of requests");
            System.out.println("3) Frequency of requests (in a second). -1 for concurrent requests");
            System.out.println("4) System type, SINGLE or CHUNK");
            System.exit(0);
        }

        // Instantiate a new Test object and start the test
        Test t = new Test(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
        t.start();
    }

    /**
     * Default constructor
     * @param _M: Number of files
     * @param _N: Number of requests
     * @param _F: Frequency of requests
     * @param _type: Type of the system. SINGLE or CHUNK.
     */
    public Test(int _M, int _N, int _F, String _type) {
        M = _M;
        N = _N;
        F = _F;
        type = _type;
        completed = new AtomicInteger(0);
        rand = new Random();
        jobQueue = Collections.synchronizedList(new ArrayList<String>());
        lookupTimes = Collections.synchronizedList(new ArrayList<Long>());
        totalBytes = Collections.synchronizedList(new ArrayList<Long>());
    }

    /**
     * Start test
     */
    public void start() {
        try {
            // Call writeTestFile and fillQueue to create a virtual test file
            // and virtual job queue
            writeTestFile();
            fillQueue();

            // Start an Indexing Server on the port 13001
            Runtime.getRuntime().exec(String.format("java RunIndexingServer 13001 indexing %s true", type));
            Thread.sleep(1000 + 1000 * (M / 10000));
            System.out.println("Test is starting....");

            // Create a thread pool to execute threads
            final ExecutorService pool = Executors.newFixedThreadPool(NUMBER_OF_CONCURRENT_THREADS);
            String job;
            TestClient tc = null;

            // Assign each job in the queue to a TestClient thread
            for (int i = 0; i < jobQueue.size(); i++) {
                if (F > 0) Thread.sleep(1000 / F); // Sleep 1000/F ms
                tc = new TestClient(this, jobQueue.get(i), i);
                pool.execute(tc);
            }

            System.out.println("Test finished...");
            System.out.println("Statistics:");

            // Wait until all threads are done
            while(completed.get() != N);

            long totalLookup = 0;
            long totalSize = 0;

            assert lookupTimes.size() == totalBytes.size();

            for (int i = 0; i < lookupTimes.size(); i++) {
                totalLookup += lookupTimes.get(i);
                totalSize += totalBytes.get(i);
            }

            // Print out the statistics
            System.out.println("Total lookup requests made by clients: " + lookupTimes.size());
            System.out.println("Average lookup time seen by clients: " + ((totalLookup * 1.0) / lookupTimes.size()));
            System.out.println("Total bytes transferred: " + totalSize);
            System.out.println("Total message exchanged: " + lookupTimes.size() * 2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Increase completed job count
     */
    public void increaseCompleted() {
        completed.incrementAndGet();
    }

    /**
     * Adds into lookupTimes list
     * @param _t lookup time to be added
     */
    public void addLookupTimes(long _t) {
        lookupTimes.add(_t);
    }

    /**
     * Adds into totalBytes list
     * @param _b byte count to be added
     */
    public void addTotalBytes(long _b) {
        totalBytes.add(_b);
    }

    /**
     * Create a virtual job queue with lookup calls
     */
    private void fillQueue() {
        for (int i = 0; i < N; i++)
            jobQueue.add(String.format("file%d.txt", rand.nextInt(M)));
    }

    /**
     * Write a virtual test file
     * This test file will be read by IndexingServer and it will fill its ConcurrentHashMap
     */
    private void writeTestFile() {
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter("test_files.txt"));
            int filesize = 0;
            int sourcesize = 0;
            ArrayList<String> sources;

            for (int i = 0; i < M; i++) {
                sources = new ArrayList<String>();
                filesize = rand.nextInt(100000000);
                sourcesize = rand.nextInt(100);
                for (int j = 0; j < sourcesize; j++)
                    sources.add(String.format("machine_%d", j));
                sources.add("machine_00");
                buf.write(String.format("file%d.txt,%d,%s\n", i, filesize, String.join(",", sources)));
            }

            buf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
