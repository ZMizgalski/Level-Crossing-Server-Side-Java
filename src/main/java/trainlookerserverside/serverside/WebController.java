package trainlookerserverside.serverside;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
import trainlookerserverside.serverside.DTOS.*;
import trainlookerserverside.serverside.objectdetection.ObjectDetectionService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@RestController
@CrossOrigin(value = "*", maxAge = 3600)
@RequestMapping(value = "/api/server")
public class WebController {

    private final static Pattern UUID_REGEX_PATTERN = Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");
    @Getter
    private final Map<UUID, String> levelCrossingIps = new HashMap<>();
    @Getter
    private final Multimap<UUID, AreaDataDTO> selectedAreas = ArrayListMultimap.create();

    public static boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }
        return UUID_REGEX_PATTERN.matcher(str).matches();
    }

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

    @DeleteMapping(value = "/deleteArea")
    public ResponseEntity<?> deleteArea(@RequestBody DeleteAreaDTO deleteAreaDTO) {
        if (!isValidUUID(deleteAreaDTO.getId())) {
            return ResponseEntity.badRequest().body(String.format("Invalid id: %s", deleteAreaDTO.getId()));
        }
        Collection<AreaDataDTO> areas = selectedAreas.get(UUID.fromString(deleteAreaDTO.getId()));
        if (areas.isEmpty()) {
            return ResponseEntity.badRequest().body(String.format("Area with level crossing id: %s not exists", deleteAreaDTO.getId()));
        }
        AreaDataDTO area = areas.parallelStream().filter(s -> s.getAreaName().equals(deleteAreaDTO.getAreaName())).findFirst().orElse(null);
        selectedAreas.remove(UUID.fromString(deleteAreaDTO.getId()), area);
        return ResponseEntity.ok().body(String.format("Area with id: %s and name: %s has ben deleted", deleteAreaDTO.getId(), deleteAreaDTO.getAreaName()));
    }

    @GetMapping(value = "/getAllAreasById/{id}")
    public ResponseEntity<?> getAllAreas(@PathVariable String id) {
        Collection<AreaDataDTO> areas = selectedAreas.get(UUID.fromString(id));
        return ResponseEntity.ok().body(areas);
    }

    @PostMapping(value = "/setArea")
    public ResponseEntity<?> setArea(@RequestBody SetAreaDTO areaDTO) {
        if (!isValidUUID(areaDTO.getId())) {
            return ResponseEntity.badRequest().body(String.format("Invalid id: %s", areaDTO.getId()));
        }
//        if (!levelCrossingIps.containsKey(UUID.fromString(areaDTO.getId()))) {
//            return ResponseEntity.badRequest().body(String.format("Lever crossing with id: %s has not been registered yet", areaDTO.getId()));
//        }
        Collection<AreaDataDTO> areaData = selectedAreas.get(UUID.fromString(areaDTO.getId()));
        if (!areaData.isEmpty() && areaData.parallelStream().anyMatch(s -> s.getAreaName().equals(areaDTO.getArea().getAreaName()))) {
            return ResponseEntity.badRequest().body("Area with that name already exists");
        }
        if (areaDTO.getArea().getPointsList().isEmpty()) {
            return ResponseEntity.badRequest().body("Points list can't be empty");
        }
        if (!areaData.isEmpty() && areaData.parallelStream().anyMatch(s -> s.getPointsList().equals(areaDTO.getArea().getPointsList()))) {
            return ResponseEntity.badRequest().body("Area can't be the same");
        }
        selectedAreas.put(UUID.fromString(areaDTO.getId()), areaDTO.getArea());
        return ResponseEntity.ok().body("Area has been set");
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
        new Thread(() -> ObjectDetectionService.runDetection(new VideoCapture("videos/" + id + ".mp4"))).start();
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
