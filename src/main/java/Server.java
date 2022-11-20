import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHanler> connections;
    private ServerSocket server;
    private boolean done;

    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHanler hanler = new ConnectionHanler(client);
                connections.add(hanler);
                pool.execute(hanler);
            }
        } catch (Exception e) {
            shutdown();

        }

    }

    public void broadcast(String message) {
        for (ConnectionHanler ch : connections) {
            if (ch != null) {
                ch.SendMessage(message);
            }

        }
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHanler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    class ConnectionHanler implements Runnable {


        private Socket clinet;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHanler(Socket client) {
            this.clinet = client;
        }

        @Override
        public void run() {
            System.out.println("please enter a nickname ");


            try {
                out = new PrintWriter(clinet.getOutputStream(), true);
                //  out.print("please enter a nickname  ");

                in = new BufferedReader(new InputStreamReader(clinet.getInputStream()));

                nickname = in.readLine();
                System.out.println(nickname + " connected");
                broadcast(nickname + " joined chat ");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + " renamed themselves to " + messageSplit[1]);
                            System.out.println(nickname + " renamed themselves to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfull  changed nickname to" + nickname);
                        } else {
                            out.println("NO nickname provided!");
                        }

                    } else if (message.startsWith("/quit")) {
                        broadcast(nickname + " left the chat ");
                        shutdown();
                    } else {
                        broadcast(nickname + ":" + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }

        }

        public void SendMessage(String message) {
            out.println(message);

        }

        public void shutdown() {

            try {
                in.close();
                out.close();
                if (!clinet.isClosed()) {
                    clinet.close();
                }
            } catch (IOException e) {
//ignore
            }

        }

    }

    public static void main(String[] args) {
        Server server1 = new Server();
        server1.run();
    }
}
