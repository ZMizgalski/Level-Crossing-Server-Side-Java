package trainlookerserverside.serverside.socket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionExceptionEvent;
import org.springframework.stereotype.Component;
import trainlookerserverside.serverside.DataService;

import java.util.Set;

@Component
public class TcpConnectionListener implements ApplicationListener<TcpConnectionEvent> {

    private final Logger log = LoggerFactory.getLogger(TcpConnectionListener.class);
    private final Set<String> connections;

    @Autowired
    private DataService dataService;

    public TcpConnectionListener(@Qualifier("tcpConnections") Set<String> connections) {
        this.connections = connections;
    }

    @Override
    public void onApplicationEvent(TcpConnectionEvent event) {
        if (event instanceof TcpConnectionExceptionEvent) {
            log.info("TCP connection closed with id={}", event.getConnectionId());
            connections.remove(event.getConnectionId());
            dataService.levelCrossingIps.values().removeIf(value -> value.getConnectionId().equals(event.getConnectionId()));
            log.info(String.valueOf(dataService.levelCrossingIps));
        }
    }
}
