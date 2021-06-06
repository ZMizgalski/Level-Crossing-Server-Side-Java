package trainlookerserverside.serverside.DTOS;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterNewLevelCrossingDTO {

    private String levelCrossingIP;

    private String id;
}
