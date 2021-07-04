package trainlookerserverside.serverside.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CameraResponse {

    private String id;
    private String ip;
    private String connectionId;
    private List<AreaDataDTO> selectedAreas;
}
