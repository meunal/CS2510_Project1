import java.io.Serializable;

public class FileMeta implements Serializable {
    private static final long serialVersionUID = -7226234188134173803L;
    private String name;
    private long size;

    public FileMeta(String _name, long _size) {
        name = _name;
        size = _size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }
}
