package trainlookerserverside.serverside;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.opencv.videoio.VideoCapture;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import trainlookerserverside.serverside.DTOS.ChangeMotorDirectionDTO;
import trainlookerserverside.serverside.DTOS.RegisterNewLevelCrossingDTO;
import trainlookerserverside.serverside.objectdetection.ObjectDetectionService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(value = "*", maxAge = 3600)
@RequestMapping(value = "/api/server")
public class WebController {

    @Getter
    private final Map<UUID, String> levelCrossingIps = new HashMap<>();

    private void addNewLevelCrossing(RegisterNewLevelCrossingDTO registerNewLevelCrossingDTO) {
        val id = UUID.fromString(registerNewLevelCrossingDTO.getId());
        val ip = registerNewLevelCrossingDTO.getLevelCrossingIP();
        if (levelCrossingIps.containsValue(ip)) {
            levelCrossingIps.values().remove(ip);
            levelCrossingIps.put(id, registerNewLevelCrossingDTO.getLevelCrossingIP());
            log.warn(String.format("Existing levelCrossing ip updated as: %s", registerNewLevelCrossingDTO.getLevelCrossingIP()));
            return;
        }
        levelCrossingIps.put(id, registerNewLevelCrossingDTO.getLevelCrossingIP());
        log.warn(String.format("New levelCrossing registered as: %s", registerNewLevelCrossingDTO.getLevelCrossingIP()));
    }

    @PostMapping(value = "/registerNewLevelCrossing")
    public ResponseEntity<?> registerNewLevelCrossing(@RequestBody RegisterNewLevelCrossingDTO registerNewLevelCrossingDTO) {
        val levelCrossingIP = registerNewLevelCrossingDTO.getLevelCrossingIP();
        addNewLevelCrossing(registerNewLevelCrossingDTO);
        System.out.println(levelCrossingIps);
        return ResponseEntity.ok().body(String.format("You have been registered as: %s", levelCrossingIP));
    }

    @PostMapping(value = "/motorControl")
    public ResponseEntity<?> motorControl(@RequestBody ChangeMotorDirectionDTO changeMotorDirectionDTO) {

        val id = UUID.fromString(changeMotorDirectionDTO.getId());
        val motorValue = changeMotorDirectionDTO.getSwitchMotor();
        val levelCrossingAddress = levelCrossingIps.get(id);

        if (motorValue == null) {
            return ResponseEntity.badRequest().body("Motor value not defined!");
        }

        if (levelCrossingAddress == null) {
            return ResponseEntity.badRequest().body("Address not exists");
        }

        if (motorValue == 1) {
            makeMotorChangeRequest("/openLevelCrossing", id, levelCrossingAddress);
        }

        if (motorValue == 0) {
            makeMotorChangeRequest("/closeLevelCrossing", id, levelCrossingAddress);
        }

        return ResponseEntity.ok().body(String.format("LevelCrossing motors with id: %s switched to: %s", id.toString(), motorValue));
    }

    private void makeMotorChangeRequest(String httpRequest, UUID id, String address) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = address + httpRequest + "/" + id;
            log.info(url);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    "",
                    String.class);
            log.warn(response.getBody());
        } catch (Exception e) {
            levelCrossingIps.remove(id);
            log.error(String.format("Can't make request: %s", httpRequest));
        }
    }

    @SneakyThrows
    @RequestMapping("/server-stream/{id}")
    @ResponseBody
    public StreamingResponseBody getSecuredHttpStream(@PathVariable String id) {
        val levelCrossingAddress = levelCrossingIps.get(UUID.fromString(id));
        if (levelCrossingAddress == null) {
            return outputStream -> {
            };
        }
        RestTemplate restTemplate = new RestTemplate();
        String url = levelCrossingAddress + "/streamCamera/" + id;
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url, HttpMethod.POST, null, Resource.class);
        FileUtils.copyInputStreamToFile(Objects.requireNonNull(responseEntity.getBody()).getInputStream(), new File("videos/" + id + ".mp4"));
        ObjectDetectionService.runDetection(new VideoCapture("videos/" + id + ".mp4"));
        InputStream st = Objects.requireNonNull(responseEntity.getBody()).getInputStream();
        return (os) -> readAndWrite(st, os);
    }

    private void readAndWrite(final InputStream is, OutputStream os)
            throws IOException {
        byte[] data = new byte[2048];
        int read;
        while ((read = is.read(data)) > 0) {
            os.write(data, 0, read);
        }
        os.flush();
    }

}
