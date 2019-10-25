import java.util.ArrayList;

public class FileSources extends FileMeta {
    private ArrayList<String> sourceList;

    public FileSources(String _name, long _size) {
        super(_name, _size);
        sourceList = new ArrayList<String>();
    }

    public FileSources(String _name, long _size, ArrayList<String> _sourceList) {
        super(_name, _size);
        sourceList = _sourceList;
    }

    public void addToSourceList(String _source) {
        sourceList.add(_source);
    }

    public ArrayList<String> getSourceList() {
        return sourceList;
    }
}
