package trainlookerserverside.serverside.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;

public class SocketHandler implements Callable<String> {

    private final BufferedReader in;
    private final Socket socketClient;
    private final PrintWriter out;
    private String ip;

    public SocketHandler(Socket socket) throws IOException {
        this.socketClient = socket;
        this.in = new BufferedReader(new InputStreamReader(this.socketClient.getInputStream()));
        this.out = new PrintWriter(this.socketClient.getOutputStream(), true);
    }

    @Override
    public String call() {
        try {
            while (true) {
                String request = in.readLine();
                if (request == null) break;
                ip = request;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            out.close();
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ip;
    }
}