package trainlookerserverside.serverside.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectionDTO {

    private String ip;

    private String connectionId;
}
