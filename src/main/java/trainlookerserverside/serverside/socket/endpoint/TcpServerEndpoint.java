package trainlookerserverside.serverside.socket.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import trainlookerserverside.serverside.socket.service.MessageService;

@MessageEndpoint
public class TcpServerEndpoint {

    private final MessageService messageService;
    private TcpConnectionEvent connection;

    @Autowired
    public TcpServerEndpoint(MessageService messageService) {
        this.messageService = messageService;
    }

    @EventListener(TcpConnectionEvent.class)
    public void getConnectionId(TcpConnectionEvent event) {
        this.connection = event;
    }

    @ServiceActivator(inputChannel = "inboundChannel")
    public byte[] process(byte[] message) {
        return messageService.processMessage(message, connection);
    }
}
