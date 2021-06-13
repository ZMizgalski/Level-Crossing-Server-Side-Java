package trainlookerserverside.serverside.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.opencv.core.Point;

import java.util.List;

@Data
@AllArgsConstructor
public class AreaDataDTO {

    private String areaName;

    private List<Point> pointsList;

}
