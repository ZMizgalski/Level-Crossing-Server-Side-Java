package trainlookerserverside.serverside.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateAreaDTO {

    private String id;

    private String oldAreaName;

    private AreaDataDTO area;
}
