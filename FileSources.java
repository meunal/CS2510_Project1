import java.util.ArrayList;

/**
 * This class extends FileMeta such that it can hold list of machines
 * that holds this particular file
 * @author Mesut Erhan Unal and Erhu He
 */
public class FileSources extends FileMeta {
    private ArrayList<String> sourceList;

    /**
     * Default constructor. Instantiates a FileSource object
     * with empty source list
     * @param _name File name
     * @param _size File size in bytess
     */
    public FileSources(String _name, long _size) {
        super(_name, _size);
        sourceList = new ArrayList<String>();
    }

    /**
     * Overloaded constructor. Instantiates a FileSource object
     * with pre-calculated source list
     * @param _name File name
     * @param _size File size
     * @param _sourceList Source list
     */
    public FileSources(String _name, long _size, ArrayList<String> _sourceList) {
        super(_name, _size);
        sourceList = _sourceList;
    }

    /**
     * Adds a peer to the source list
     * @param _source IP and port information of the peer (e.g. 192.168.1.1:6505)
     */
    public void addToSourceList(String _source) {
        sourceList.add(_source);
    }

    /**
     * Source list getter
     * @return Source list
     */
    public ArrayList<String> getSourceList() {
        return sourceList;
    }
}
