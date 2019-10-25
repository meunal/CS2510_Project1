public abstract class Server {
    protected int port;
    protected String serverID;
    protected SystemType type;

    abstract void start();

    public Server(int _port, String _serverID, SystemType _type) {
        port = _port;
        serverID = _serverID;
        type = _type;
    }

    public Server(int _port, String _serverID) {
        port = _port;
        serverID = _serverID;
        type = null;
    }

    public int getPort() {
        return port;
    }

    public String getServerID() {
        return serverID;
    }

    public SystemType getType() {
        return type;
    }
}

