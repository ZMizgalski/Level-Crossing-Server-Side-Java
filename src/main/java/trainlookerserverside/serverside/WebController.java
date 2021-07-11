package trainlookerserverside.serverside;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.opencv.videoio.VideoCapture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.config.Task;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import trainlookerserverside.serverside.DTOS.*;
import trainlookerserverside.serverside.objectdetection.ObjectDetectionService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static trainlookerserverside.serverside.DataService.isValidUUID;

@Slf4j
@RestController
@CrossOrigin(value = "*", maxAge = 3600)
@RequestMapping(value = "/api/server")
public class WebController {

    @Autowired
    private DataService dataService;

    @Autowired
    private ObjectDetectionService objectDetectionService;

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

    @GetMapping(value = "/getAllCameras")
    public ResponseEntity<?> getAllCameras() {
        List<AllCamerasResponse> cameras = new ArrayList<>();
        this.dataService.levelCrossingIps.forEach((ip, connectionId) -> {
            AllCamerasResponse allCamerasResponse = new AllCamerasResponse(
                    ip.toString(),
                    connectionId.getData() == null ? "Default content" : connectionId.getData());
            cameras.add(allCamerasResponse);
        });
        return ResponseEntity.ok().body(cameras);
    }

    @GetMapping(value = "/getCameraById/{id}")
    public ResponseEntity<?> getCameraById(@PathVariable String id) {
        if (!isValidUUID(id)) {
            return ResponseEntity.badRequest().body("Invalid UUID");
        }
        if (!this.dataService.levelCrossingIps.containsKey(UUID.fromString(id))) {
            return ResponseEntity.badRequest().body("Camera not exists");
        }
        ConnectionDTO connectionDTO = this.dataService.levelCrossingIps.get(UUID.fromString(id));
        CameraResponse cameraResponse = new CameraResponse(
                id,
                connectionDTO.getIp(),
                connectionDTO.getConnectionId()
        );
        return ResponseEntity.ok().body(cameraResponse);
    }

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
            dataService.makeMotorChangeRequest("/openLevelCrossing", id, levelCrossingAddress.getIp());
        }
        if (motorValue == 0) {
            dataService.makeMotorChangeRequest("/closeLevelCrossing", id, levelCrossingAddress.getIp());
        }
        return ResponseEntity.ok().body(String.format("LevelCrossing motors with id: %s switched to: %s", id.toString(), motorValue));
    }

//    private void makeMotorChangeRequest(String httpRequest, UUID id, String address) {
//        try {
//            RestTemplate restTemplate = new RestTemplate();
//            String url = address + httpRequest + "/" + id;
//            log.info(url);
//            ResponseEntity<String> response = restTemplate.postForEntity(
//                    url,
//                    "",
//                    String.class);
//            log.warn(response.getBody());
//        } catch (Exception e) {
//            dataService.levelCrossingIps.remove(id);
//            log.error(String.format("Can't make request: %s", httpRequest));
//        }
//    }

    @PutMapping(value = "/updateArea")
    public ResponseEntity<?> updateArea(@RequestBody UpdateAreaDTO updateAreaDTO) {
        if (!isValidUUID(updateAreaDTO.getId())) {
            return ResponseEntity.badRequest().body(String.format("UUID: %s is not valid", updateAreaDTO.getId()));
        }
        if (!dataService.selectedAreas.containsKey(UUID.fromString(updateAreaDTO.getId()))) {
            return ResponseEntity.badRequest().body("Area not exists!");
        }
        Collection<AreaDataDTO> areaDataDTOS = dataService.selectedAreas.get(UUID.fromString(updateAreaDTO.getId()));
        areaDataDTOS.removeIf(areaDataDTO -> areaDataDTO.getAreaName().equals(updateAreaDTO.getOldAreaName()));
        dataService.selectedAreas.put(UUID.fromString(updateAreaDTO.getId()), updateAreaDTO.getArea());
        return ResponseEntity.ok().body(String.format("Area with id: %s has been updated", updateAreaDTO.getId()));
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
    @GetMapping(value = "/getFileByDate/{id}/{date}")
    public StreamingResponseBody getFile(@PathVariable("id") String id, @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd_HH-mm-ss") Date date) {
        ConnectionDTO connectionDTO = dataService.levelCrossingIps.get(UUID.fromString(id));
        if (connectionDTO == null) {
            return outputStream -> {
            };
        }
        SimpleDateFormat workDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        RestTemplate restTemplate = new RestTemplate();
        String url = connectionDTO.getIp() + "/getFileByDate/" + workDateFormat.format(date);
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Resource.class);
        InputStream st = Objects.requireNonNull(responseEntity.getBody()).getInputStream();
        return (os) -> dataService.readAndWrite(st, os);
    }

    @SneakyThrows
    @GetMapping(value = "/downloadFileByDate/{id}/{date}")
    public StreamingResponseBody downloadFile(@PathVariable("id") String id, @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd_HH-mm-ss") Date date) {
        ConnectionDTO connectionDTO = dataService.levelCrossingIps.get(UUID.fromString(id));
        if (connectionDTO == null) {
            return outputStream -> {
            };
        }
        SimpleDateFormat workDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        RestTemplate restTemplate = new RestTemplate();
        String url = connectionDTO.getIp() + "/downloadFileByDate/" + workDateFormat.format(date);
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Resource.class);
        InputStream st = Objects.requireNonNull(responseEntity.getBody()).getInputStream();
        return (os) -> dataService.readAndWrite(st, os);
    }

    @SneakyThrows
    @GetMapping(value = "/getFilesByDay/{id}/{date}")
    public StreamingResponseBody getFiles(@PathVariable("id") String id, @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        ConnectionDTO connectionDTO = dataService.levelCrossingIps.get(UUID.fromString(id));
        if (connectionDTO == null) {
            return outputStream -> {
            };
        }
        SimpleDateFormat workDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        RestTemplate restTemplate = new RestTemplate();
        String url = connectionDTO.getIp() + "/getFilesByDay/" + workDateFormat.format(date) + "/"+ id;
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Resource.class);
        InputStream st = Objects.requireNonNull(responseEntity.getBody()).getInputStream();
        return (os) -> dataService.readAndWrite(st, os);
    }

    @GetMapping(value = "/getAllAreasById/{id}")
    public ResponseEntity<?> getAllAreas(@PathVariable String id) {
        Collection<AreaDataDTO> areas = dataService.selectedAreas.get(UUID.fromString(id));
        List<SetAreaDTO> finalAreas = new ArrayList<>();
        areas.forEach(item -> {
            finalAreas.add(new SetAreaDTO(id, item));
        });
        return ResponseEntity.ok().body(finalAreas);
    }

    @SneakyThrows
    @RequestMapping("/stream-cover/{id}")
    @ResponseBody
    public StreamingResponseBody getStreamCover(@PathVariable String id) {
        val levelCrossingAddress = dataService.levelCrossingIps.get(UUID.fromString(id));
        if (levelCrossingAddress == null) {
            return outputStream -> {
            };
        }
        RestTemplate restTemplate = new RestTemplate();
        String url = levelCrossingAddress.getIp() + "/getStreamCover";
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Resource.class);
        InputStream st = Objects.requireNonNull(responseEntity.getBody()).getInputStream();
        return (os) -> dataService.readAndWrite(st, os);
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
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Resource.class);
        Calendar currentUtilCalendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss");
        String date = dateFormat.format(currentUtilCalendar.getTime());
        String path = "videos/" + date + ".mp4";
        FileUtils.copyInputStreamToFile(Objects.requireNonNull(responseEntity.getBody()).getInputStream(), new File(path));
        Collection<AreaDataDTO> selectedAreas = dataService.selectedAreas.get(UUID.fromString(id));
        Thread thread = new Thread(() -> objectDetectionService.runDetection(new VideoCapture(path), path, selectedAreas, id));
        thread.start();
        InputStream st = Objects.requireNonNull(responseEntity.getBody()).getInputStream();
        return (os) -> dataService.readAndWrite(st, os);
    }

//    private void readAndWrite(final InputStream is, OutputStream os)
//            throws IOException {
//        byte[] data = new byte[2048];
//        int read;
//        while ((read = is.read(data)) > 0) {
//            os.write(data, 0, read);
//        }
//        os.flush();
//    }
}
