package trainlookerserverside.serverside;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import trainlookerserverside.serverside.objectdetection.ObjectDetectionService;

@SpringBootApplication
public class ServersideApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(ServersideApplication.class, args);
        ObjectDetectionService.runDetection();
    }

}
