package trainlookerserverside.serverside.socket;

import lombok.extern.slf4j.Slf4j;
import trainlookerserverside.serverside.DTOS.ConnectionDTO;
import trainlookerserverside.serverside.DataService;

import java.io.DataInputStream;
import java.net.Socket;
import java.util.UUID;

@Slf4j
public class SocketClientHandler extends Thread {
    private final DataService dataService;
    protected Socket socket;
    private boolean connected;
    private boolean dataReceived;
    private String lastMessage;

    public SocketClientHandler(Socket socket, DataService dataService) {
        this.dataService = dataService;
        this.socket = socket;
        this.lastMessage = "";
        this.connected = true;
        this.dataReceived = false;
    }

    public void run() {
        try {
            System.out.println("Receive new connection: " + socket.getInetAddress().toString().replaceAll("/", ""));
            DataInputStream in = new DataInputStream(socket.getInputStream());
            while (connected) {
                try {
                    String message = in.readUTF();
                    if ((!message.isBlank() || !message.isEmpty()) && !dataReceived) {
                        dataReceived = true;
                        lastMessage = message;
                        dataService.levelCrossingIps.entrySet().removeIf(item -> item.getValue().getIp().equals(message));
                        UUID connectionUUID = UUID.randomUUID();
                        dataService.levelCrossingIps.put(connectionUUID, new ConnectionDTO(message, "Default Content", connectionUUID.toString()));
                        log.info(String.valueOf(dataService.levelCrossingIps));
                    }
                } catch (Exception e) {
                    connected = false;
                    dataReceived = false;
                    log.info("TCP connection closed with ip={}", lastMessage);
                    String finalLastMessage = lastMessage;
                    dataService.levelCrossingIps.entrySet().removeIf(item -> item.getValue().getIp().equals(finalLastMessage));
                    log.info(String.valueOf(dataService.levelCrossingIps));
                    System.out.println("Client Disconnected!");
                }
            }
            socket.close();
        } catch (Exception e) {
            System.out.println("Closing thread..");
            e.printStackTrace();
            this.connected = false;
        }
    }
}
