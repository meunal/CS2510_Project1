public class RunIndexingServer {
    public static void main(String [] args) {
        if (args.length != 3) {
            System.out.println("RunIndexingServer expects 3 arguments:");
            System.out.println("1) Port Number");
            System.out.println("2) Server ID (any string that can distinguish this server)");
            System.out.println("3) System type (SINGLE or CHUNK)");
            System.exit(0);
        }

        SystemType type = args[2].equals("SINGLE") ? SystemType.SINGLE : SystemType.CHUNK;
        IndexingServer is = new IndexingServer(Integer.parseInt(args[0]), args[1], type);
        is.start();
    }
}
