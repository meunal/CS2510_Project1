import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    private static final long serialVersionUID = -7226234188134173803L;
    private String message;
    private boolean EOF;
    private ArrayList<Object> content;

    public Message(String _message) {
        message = _message;
        content = new ArrayList<Object>();
    }

    public void addContent(Object o) {
        content.add(o);
    }

    public void setEOF(boolean _EOF) {
        EOF = _EOF;
    }

    public String getMessage() {
        return message;
    }

    public ArrayList<Object> getContent() {
        return content;
    }

    public boolean getEOF() {
        return EOF;
    }
}
