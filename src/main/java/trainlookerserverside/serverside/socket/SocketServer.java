package trainlookerserverside.serverside.socket;

import lombok.extern.slf4j.Slf4j;
import trainlookerserverside.serverside.DataService;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class SocketServer implements Runnable {
    private final String serverIp;
    private final Integer serverPort;
    private final DataService dataService;
    private boolean shutdown;

    public SocketServer(Integer serverPort, DataService dataService) {
        this.dataService = dataService;
        this.shutdown = false;
        this.serverPort = serverPort;
        this.serverIp = "localhost";
    }

    public SocketServer(String serverIp, Integer serverPort, DataService dataService) {
        this.dataService = dataService;
        this.shutdown = false;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void stop() {
        this.shutdown = true;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket();
            InetSocketAddress address1 = new InetSocketAddress(this.serverIp, this.serverPort);
            serverSocket.bind(address1);
            while (!shutdown) {
                try {
                    Socket soc = serverSocket.accept();
                    new SocketClientHandler(soc, dataService).start();
                } catch (Exception e) {
                    System.out.println("Failed to accept socket connection");
                }
            }
        } catch (Exception e) {
            System.out.println("Closing thread..");
            e.printStackTrace();
            this.shutdown = true;
        }
    }
}
