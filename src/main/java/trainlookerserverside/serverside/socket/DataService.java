package trainlookerserverside.serverside.socket;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import trainlookerserverside.serverside.DTOS.AreaDataDTO;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

@Service
public class DataService {

    @Getter
    public final Map<UUID, String> levelCrossingIps = new HashMap<>();
    @Getter
    public final Multimap<UUID, AreaDataDTO> selectedAreas = ArrayListMultimap.create();
    @Getter
    private final ArrayList<SocketHandler> socketHandlers = new ArrayList<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
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
    private void start(int port) {
        serverSocket = new ServerSocket(port);
        while (true) {
            Socket socket = serverSocket.accept();
            SocketHandler socketThread = new SocketHandler(socket);
            socketHandlers.add(socketThread);
            Future<String> future = pool.submit(socketThread);
            String ip = future.get();
            levelCrossingIps.values().removeIf(value -> value.equals(ip));
            levelCrossingIps.put(UUID.randomUUID(), ip);
            System.out.println(levelCrossingIps);
        }
    }

    @SneakyThrows
    public void startServerSocket() {
        start(localPort);
    }
}