package trainlookerserverside.serverside.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SetAreaDTO {

    private String id;

    private AreaDataDTO area;
}
