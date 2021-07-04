package trainlookerserverside.serverside.socket.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.stereotype.Service;
import trainlookerserverside.serverside.DTOS.ConnectionDTO;
import trainlookerserverside.serverside.DataService;
import trainlookerserverside.serverside.socket.service.MessageService;

import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private DataService dataService;

    @Override
    public byte[] processMessage(byte[] message, TcpConnectionEvent connection) {
        String messageContent = new String(message);
        LOGGER.info("Receive message: {}", messageContent);
        ConnectionDTO connectionDTO = new ConnectionDTO(messageContent, "Default Content", connection.getConnectionId());
        if (!dataService.levelCrossingIps.containsValue(connectionDTO)) {
            dataService.levelCrossingIps.put(UUID.randomUUID(), connectionDTO);
        }
        String responseContent = String.format("Message \"%s\" is processed", messageContent);
        LOGGER.info(String.valueOf(dataService.levelCrossingIps));
        return responseContent.getBytes();
    }

}
