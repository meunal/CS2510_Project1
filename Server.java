/**
 * Abstract server class
 * Holds port, ID and type information
 * @author Mesut Erhan Unal and Erhu He
 */
public abstract class Server {
    protected int port;
    protected String serverID;
    protected SystemType type;

    /**
     * Default constructor
     * @param _port Port number
     * @param _serverID ID of the server that distinguish this process from the others
     */
    public Server(int _port, String _serverID) {
        port = _port;
        serverID = _serverID;
        type = null;
    }

    /**
     * Overloaded constructor with type information
     * @param _port Port number
     * @param _serverID ID of the server that distinguish this process from the others
     * @param _type System type
     */
    public Server(int _port, String _serverID, SystemType _type) {
        port = _port;
        serverID = _serverID;
        type = _type;
    }

    /**
     * Abstract start method
     */
    abstract void start();

    /**
     * Port number getter
     * @return Port number
     */
    public int getPort() {
        return port;
    }

    /**
     * ServerID getter
     * @return ServerID
     */
    public String getServerID() {
        return serverID;
    }

    /**
     * Type getter
     * @return System type
     */
    public SystemType getType() {
        return type;
    }
}
