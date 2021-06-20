package trainlookerserverside.serverside.socket;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import trainlookerserverside.serverside.DTOS.AreaDataDTO;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DataService {

    @Getter
    public final Map<UUID, String> levelCrossingIps = new HashMap<>();
    @Getter
    public final Multimap<UUID, AreaDataDTO> selectedAreas = ArrayListMultimap.create();
    @Getter
    private final ExecutorService pool = Executors.newFixedThreadPool(10);
    @Value("${socket.port}")
    private int localPort;
    @Getter
    private ServerSocket serverSocket;

    public static boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }
        return Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$").matcher(str).matches();
    }

    @SneakyThrows
    private boolean connectionIsReachable(String address) {
        String[] splitAddress = address.split(":");
        String address2 = splitAddress[1].replaceAll("//", "");
        Socket s;
        try {
            s = new Socket(address2, Integer.parseInt(splitAddress[2]));
        } catch (IOException e) {
            return false;
        }
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        out.write("");
        out.close();
        return true;
    }

    private void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!serverSocket.isClosed()) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException ignored) {
            } finally {
                levelCrossingIps.values().removeIf(ipAddr -> !connectionIsReachable(ipAddr));
            }
            if (socket != null) {
                SocketHandler socketThread = null;
                try {
                    socketThread = new SocketHandler(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert socketThread != null;
                Future<String> future = pool.submit(socketThread);
                String ip = null;
                try {
                    ip = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (ip != null) {
                    String finalIp = ip;
                    levelCrossingIps.values().removeIf(value -> value.equals(finalIp));
                    levelCrossingIps.put(UUID.randomUUID(), ip);
                    log.warn(String.format("LevelCrossing registered as: %s", ip));
                    System.out.println(levelCrossingIps);
                }
                try {
                    serverSocket.setSoTimeout(4000);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SneakyThrows
    public void startServerSocket() {
        start(localPort);
    }
}