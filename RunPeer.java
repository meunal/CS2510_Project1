public class RunPeer {
    public static void main(String [] args) {
        if (args.length != 4) {
            System.out.println("RunPeer expects 4 arguments:");
            System.out.println("1) Port Number");
            System.out.println("2) Peer ID (A number that can distinguish this peer from others. There needs to be a directory with the same name)");
            System.out.println("3) Indexing Server IP");
            System.out.println("4) Indexing Server Port");
            System.exit(0);
        }

        PeerServer is = new PeerServer(Integer.parseInt(args[0]), args[1], args[2], Integer.parseInt(args[3]));
        is.start();
    }
}