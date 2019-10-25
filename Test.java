import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Test {
    public static void main(String [] args) {
        try{
            Socket sock = new Socket("192.168.1.212", 13002);
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
            Message req = new Message("DOWNLOAD");
            req.addContent(new FileMeta("CS2510.pdf", 53589));
            oos.writeObject(req);

            Message resp = (Message) ois.readObject();
            byte [] file = (byte []) resp.getContent().get(0);
            FileOutputStream fos = new FileOutputStream("./download.pdf");
            fos.write(file);
            fos.close();

            oos.close();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
