package trainlookerserverside.serverside.socket.service;


import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;

public interface MessageService {

    byte[] processMessage(byte[] message, TcpConnectionEvent connection);

}
