package trainlookerserverside.serverside.socket;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import trainlookerserverside.serverside.DataService;

@Component
public class SocketConnection implements ApplicationListener<ApplicationStartedEvent>, ApplicationContextAware {

    @Value("${tcp.ip}")
    private String ip;
    @Value("${tcp.port}")
    private int port;
    private ApplicationContext context;
    @Autowired
    private DataService dataService;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if (this.port == 0 || this.ip == null || this.ip.isEmpty() || this.ip.isBlank()) {
            ((ConfigurableApplicationContext) context).close();
            System.out.println("server.ip or server.port isn't defined");
            return;
        }
        SocketServer socketServer = new SocketServer(this.ip, this.port, this.dataService);
        Thread thread = new Thread(socketServer);
        thread.start();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
