package trainlookerserverside.serverside;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import trainlookerserverside.serverside.DTOS.AreaDataDTO;
import trainlookerserverside.serverside.DTOS.ChangeMotorDirectionDTO;
import trainlookerserverside.serverside.DTOS.ConnectionDTO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DataService {

    @Getter
    public final Multimap<UUID, AreaDataDTO> selectedAreas = ArrayListMultimap.create();
    @Getter
    private final ExecutorService pool = Executors.newFixedThreadPool(10);
    @Getter
    public Map<UUID, ConnectionDTO> levelCrossingIps = new HashMap<>();

    public static boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }
        return Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$").matcher(str).matches();
    }

    public void controlMotors(ChangeMotorDirectionDTO changeMotorDirectionDTO) {
        val id = UUID.fromString(changeMotorDirectionDTO.getId());
        val motorValue = changeMotorDirectionDTO.getSwitchMotor();
        val levelCrossingAddress = levelCrossingIps.get(id);
        if (motorValue == null || levelCrossingAddress == null) {
            return;
        }
        if (motorValue == 1) {
            makeMotorChangeRequest("/openLevelCrossing", id, levelCrossingAddress.getIp());
        }
        if (motorValue == 0) {
            makeMotorChangeRequest("/closeLevelCrossing", id, levelCrossingAddress.getIp());
        }
    }

    public void makeMotorChangeRequest(String httpRequest, UUID id, String address) {
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

    public void readAndWrite(final InputStream is, OutputStream os)
            throws IOException {
        byte[] data = new byte[2048];
        int read;
        while ((read = is.read(data)) > 0) {
            os.write(data, 0, read);
        }
        os.flush();
    }
}