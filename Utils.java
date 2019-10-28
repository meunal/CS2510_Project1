import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Utils class for common methods
 * @author Mesut Erhan Unal
 */
public class Utils {
    /**
     * Calculate byte size of an object
     * @param o: Object whose size to be calculated
     * @return Size
     */
    public static long calculateBytes(Object o) {
        long total = 0;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.flush();
            oos.close();
            total = baos.toByteArray().length;
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    /**
     * Calculates IP address of the machine. If InetAddress gives the loopback (127.0.0.1)
     * it opens a UDP socket to 8.8.8.8 and captures machine's global IP
     * @return IP address of the machine
     */
    public static String getIP() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress().trim();
            if (!ip.equals("127.0.0.1")) return ip;
            final DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
