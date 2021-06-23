package trainlookerserverside.serverside;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.opencv.videoio.VideoCapture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static trainlookerserverside.serverside.DataService.isValidUUID;

@Slf4j
@RestController
@CrossOrigin(value = "*", maxAge = 3600)
@RequestMapping(value = "/api/server")
public class WebController {

    @Autowired
    private DataService dataService;

//    @PostMapping(value = "/registerNewLevelCrossing")
//    public ResponseEntity<?> registerNewLevelCrossing(@RequestBody RegisterNewLevelCrossingDTO registerNewLevelCrossingDTO) {
//        val levelCrossingIP = registerNewLevelCrossingDTO.getLevelCrossingIP();
//        addNewLevelCrossing(registerNewLevelCrossingDTO);
//        System.out.println(dataService.levelCrossingIps);
//        return ResponseEntity.ok().body(String.format("You have been registered as: %s", levelCrossingIP));
//    }

//    private void addNewLevelCrossing(RegisterNewLevelCrossingDTO registerNewLevelCrossingDTO) {
//        val id = UUID.fromString(registerNewLevelCrossingDTO.getId());
//        val ip = registerNewLevelCrossingDTO.getLevelCrossingIP();
//        if (dataService.levelCrossingIps.containsValue(ip)) {
//            dataService.levelCrossingIps.values().remove(ip);
//            dataService.levelCrossingIps.put(id, registerNewLevelCrossingDTO.getLevelCrossingIP());
//            log.warn(String.format("Existing levelCrossing ip updated as: %s", registerNewLevelCrossingDTO.getLevelCrossingIP()));
//            return;
//        }
//        dataService.levelCrossingIps.put(id, registerNewLevelCrossingDTO.getLevelCrossingIP());
//        log.warn(String.format("New levelCrossing registered as: %s", registerNewLevelCrossingDTO.getLevelCrossingIP()));
//    }


    @PostMapping(value = "/motorControl")
    public ResponseEntity<?> motorControl(@RequestBody ChangeMotorDirectionDTO changeMotorDirectionDTO) {
        val id = UUID.fromString(changeMotorDirectionDTO.getId());
        val motorValue = changeMotorDirectionDTO.getSwitchMotor();
        val levelCrossingAddress = dataService.levelCrossingIps.get(id);
        if (motorValue == null) {
            return ResponseEntity.badRequest().body("Motor value not defined!");
        }
        if (levelCrossingAddress == null) {
            return ResponseEntity.badRequest().body("Address not exists");
        }
        if (motorValue == 1) {
            makeMotorChangeRequest("/openLevelCrossing", id, levelCrossingAddress.getIp());
        }
        if (motorValue == 0) {
            makeMotorChangeRequest("/closeLevelCrossing", id, levelCrossingAddress.getIp());
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
            dataService.levelCrossingIps.remove(id);
            log.error(String.format("Can't make request: %s", httpRequest));
        }
    }

    @PutMapping(value = "/updateArea")
    public ResponseEntity<?> updateArea(@RequestBody SetAreaDTO setAreaDTO) {
        if (!isValidUUID(setAreaDTO.getId())) {
            return ResponseEntity.badRequest().body(String.format("UUID: %s is not valid", setAreaDTO.getId()));
        }
        if (!dataService.selectedAreas.containsKey(UUID.fromString(setAreaDTO.getId()))) {
            return ResponseEntity.badRequest().body("Area not exists!");
        }
        Collection<AreaDataDTO> areaDataDTOS = dataService.selectedAreas.get(UUID.fromString(setAreaDTO.getId()));
        areaDataDTOS.removeIf(areaDataDTO -> areaDataDTO.getAreaName().equals(setAreaDTO.getArea().getAreaName()));
        dataService.selectedAreas.put(UUID.fromString(setAreaDTO.getId()), setAreaDTO.getArea());
        return ResponseEntity.ok().body(String.format("Area with id: %s has been updated", setAreaDTO.getId()));
    }

    @DeleteMapping(value = "/deleteArea")
    public ResponseEntity<?> deleteArea(@RequestBody DeleteAreaDTO deleteAreaDTO) {
        if (!isValidUUID(deleteAreaDTO.getId())) {
            return ResponseEntity.badRequest().body(String.format("Invalid id: %s", deleteAreaDTO.getId()));
        }
        Collection<AreaDataDTO> areas = dataService.selectedAreas.get(UUID.fromString(deleteAreaDTO.getId()));
        if (areas.isEmpty()) {
            return ResponseEntity.badRequest().body(String.format("Area with level crossing id: %s not exists", deleteAreaDTO.getId()));
        }
        AreaDataDTO area = areas.parallelStream().filter(s -> s.getAreaName().equals(deleteAreaDTO.getAreaName())).findFirst().orElse(null);
        dataService.selectedAreas.remove(UUID.fromString(deleteAreaDTO.getId()), area);
        return ResponseEntity.ok().body(String.format("Area with id: %s and name: %s has ben deleted", deleteAreaDTO.getId(), deleteAreaDTO.getAreaName()));
    }

    @PostMapping(value = "/setArea")
    public ResponseEntity<?> setArea(@RequestBody SetAreaDTO areaDTO) {
        if (!isValidUUID(areaDTO.getId())) {
            return ResponseEntity.badRequest().body(String.format("Invalid id: %s", areaDTO.getId()));
        }
//        if (!levelCrossingIps.containsKey(UUID.fromString(areaDTO.getId()))) {
//            return ResponseEntity.badRequest().body(String.format("Lever crossing with id: %s has not been registered yet", areaDTO.getId()));
//        }
        Collection<AreaDataDTO> areaData = dataService.selectedAreas.get(UUID.fromString(areaDTO.getId()));
        if (!areaData.isEmpty() && areaData.parallelStream().anyMatch(s -> s.getAreaName().equals(areaDTO.getArea().getAreaName()))) {
            return ResponseEntity.badRequest().body("Area with that name already exists");
        }
        if (areaDTO.getArea().getPointsList().isEmpty()) {
            return ResponseEntity.badRequest().body("Points list can't be empty");
        }
        if (!areaData.isEmpty() && areaData.parallelStream().anyMatch(s -> s.getPointsList().equals(areaDTO.getArea().getPointsList()))) {
            return ResponseEntity.badRequest().body("Points list can't be the same");
        }
        dataService.selectedAreas.put(UUID.fromString(areaDTO.getId()), areaDTO.getArea());
        return ResponseEntity.ok().body("Area has been set");
    }

    @SneakyThrows
    @GetMapping(value = "/getFileByDate/{date}")
    public ResponseEntity<?> getFile(@PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd_HH-mm-ss") Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH-mm-ss");
        String formattedDate = dateFormat.format(date);
        HttpHeaders headers = new HttpHeaders();
        File file = new File("videos/" + formattedDate + ".mp4");
        if (!file.exists()) {
            return ResponseEntity.badRequest().body("file not exists!");
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        String type = FilenameUtils.getExtension(dateFormat + ".mp4");
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("video/" + type))
                .header(HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + file.getName() + "\"")
                .body(bytes);
    }

    @SneakyThrows
    @GetMapping(value = "/getFilesByDay/{date}")
    public ResponseEntity<?> getFiles(@PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String formattedDate = dateFormat.format(date);
        Set<String> elo = Stream.of(Objects.requireNonNull(new File("videos/" + formattedDate).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
        return ResponseEntity.ok().body(elo);
    }

    @GetMapping(value = "/getAllAreasById/{id}")
    public ResponseEntity<?> getAllAreas(@PathVariable String id) {
        Collection<AreaDataDTO> areas = dataService.selectedAreas.get(UUID.fromString(id));
        return ResponseEntity.ok().body(areas);
    }

    @SneakyThrows
    @RequestMapping("/server-stream/{id}")
    @ResponseBody
    public StreamingResponseBody getSecuredHttpStream(@PathVariable String id) {
        val levelCrossingAddress = dataService.levelCrossingIps.get(UUID.fromString(id));
        if (levelCrossingAddress == null) {
            return outputStream -> {
            };
        }
        RestTemplate restTemplate = new RestTemplate();
        String url = levelCrossingAddress.getIp() + "/streamCamera/" + id;
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url, HttpMethod.POST, null, Resource.class);
        Calendar currentUtilCalendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH-mm-ss");
        String date = dateFormat.format(currentUtilCalendar.getTime());
        FileUtils.copyInputStreamToFile(Objects.requireNonNull(responseEntity.getBody()).getInputStream(), new File("videos/" + date + ".mp4"));
        new Thread(() -> ObjectDetectionService.runDetection(new VideoCapture("videos/" + date + ".mp4"))).start();
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
