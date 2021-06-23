package trainlookerserverside.serverside;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trainlookerserverside.serverside.DTOS.AreaDataDTO;
import trainlookerserverside.serverside.DTOS.ConnectionDTO;

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
}