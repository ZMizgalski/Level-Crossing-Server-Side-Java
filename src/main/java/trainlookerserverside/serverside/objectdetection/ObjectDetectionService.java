package trainlookerserverside.serverside.objectdetection;

import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import trainlookerserverside.serverside.DTOS.AreaDataDTO;
import trainlookerserverside.serverside.DTOS.ChangeMotorDirectionDTO;
import trainlookerserverside.serverside.DataService;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ObjectDetectionService {

    public List<Integer> detectionList = new ArrayList<>();
    public int detectionCounter = 0;

    @Autowired
    private DataService dataService;

    @SneakyThrows
    public void runDetection(VideoCapture capture, String path, Collection<AreaDataDTO> selectedAreas,String id) {
        Mat frame = new Mat();
        double width = capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        double height = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        float confThreshold = 0.15f;
        float maxThreshold = 0.2f;
        List<String> detectedObjectsNames = loadCocoNamesFromFile(new ClassPathResource("deepLerningModels/coco.names").getFile());
        File modelAIConfigFile = new ClassPathResource("deepLerningModels/yolov3-tiny.cfg").getFile();
        File modelAIWeights = new ClassPathResource("deepLerningModels/yolov3-tiny.weights").getFile();
        Net net = Dnn.readNetFromDarknet(modelAIConfigFile.getAbsolutePath(), modelAIWeights.getAbsolutePath());
        net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
        net.setPreferableTarget(Dnn.DNN_TARGET_CPU);
        JFrame jframe = new JFrame("Testing");
        jframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JLabel vidPanel = new JLabel();
        jframe.setContentPane(vidPanel);
        jframe.setSize((int) width, (int) height);
        jframe.setVisible(true);
        while (true) {
            if (capture.read(frame)) {
                ImageIcon image = new ImageIcon(Mat2BufferedImage(onCameraFrame(frame, net, maxThreshold, confThreshold, detectedObjectsNames, selectedAreas, id)));
                vidPanel.setIcon(image);
                vidPanel.repaint();
            } else {
                jframe.dispose();
                new File(path).delete();
                detectionCounter = 0;
                detectionList.clear();
                break;
            }
        }
    }

    @SneakyThrows
    private List<String> loadCocoNamesFromFile(File cocoNamesFile) {
        List<String> detectedObjectsFile = new ArrayList<>();
        Scanner myReader = new Scanner(cocoNamesFile);
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            detectedObjectsFile.add(data);
        }
        myReader.close();
        return detectedObjectsFile;
    }

    private BufferedImage Mat2BufferedImage(Mat m) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b);
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    private List<String> getOutBlobNames() {
        List<String> outBlobNames = new ArrayList<>();
        outBlobNames.add(0, "yolo_16");
        outBlobNames.add(1, "yolo_23");
        return outBlobNames;
    }

    @SneakyThrows
    private Mat onCameraFrame(Mat frame, Net net, float maxThreshold, float confidenceThreshold, List<String> cocoNames, Collection<AreaDataDTO> selectedAreas, String id) {
        List<Mat> results = new ArrayList<>(2);
        List<String> outBlobNames = getOutBlobNames();
        List<Integer> clsIds = new ArrayList<>();
        List<Float> floatsConfidences = new ArrayList<>();
        List<Rect2d> rect2ds = new ArrayList<>();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        Mat blob = Dnn.blobFromImage(frame, 1 / 255f, new Size(416, 416), new Scalar(0, 0, 0), false, false);
        net.setInput(blob);
        net.forward(results, outBlobNames);
        List<MatOfPoint> points = new ArrayList<>();
        selectedAreas.forEach(areaDataDTO -> {
            MatOfPoint matOfPoint = new MatOfPoint();
            matOfPoint.fromList(areaDataDTO.getPointsList());
            points.add(matOfPoint);
        });
        double opacity = 0.6;
        Mat overlay = new Mat();
        frame.copyTo(overlay);
        Imgproc.fillPoly(frame, points, new Scalar(255, 0, 0, 0), Imgproc.LINE_8);
        Core.addWeighted(overlay, opacity, frame, 1 - opacity, 0, frame);
        for (Mat level : results) {
            for (int i = 0; i < level.rows(); ++i) {
                Mat row = level.row(i);
                Mat colRange = row.colRange(5, level.cols());
                Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(colRange);
                float confidence = (float) minMaxLocResult.maxVal;
                Point classIdPoint = minMaxLocResult.maxLoc;
                if (confidence > maxThreshold) {
                    int x = (int) (row.get(0, 0)[0] * frame.cols());
                    int y = (int) (row.get(0, 1)[0] * frame.rows());
                    int w = (int) (row.get(0, 2)[0] * frame.cols());
                    int h = (int) (row.get(0, 3)[0] * frame.rows());
                    int l = x - w / 2;
                    int t = y - h / 2;
                    clsIds.add((int) classIdPoint.x);
                    floatsConfidences.add(confidence);
                    rect2ds.add(new Rect2d(l, t, w, h));
                }
            }
        }
        int confidencesSize = floatsConfidences.size();
        if (confidencesSize >= 1) {
            MatOfFloat confidencesForDNN = new MatOfFloat(Converters.vector_float_to_Mat(floatsConfidences));
            Rect2d[] boxesForDNN = rect2ds.toArray(new Rect2d[0]);
            MatOfRect2d matOfRect2d = new MatOfRect2d(boxesForDNN);
            MatOfInt indices = new MatOfInt();
            Dnn.NMSBoxes(matOfRect2d, confidencesForDNN, maxThreshold, confidenceThreshold, indices);
            int[] ind = indices.toArray();
            for (int idx : ind) {
                Rect2d box = boxesForDNN[idx];
                int classID = clsIds.get(idx);
                for (MatOfPoint point : points) {
                    String className = cocoNames.get(classID);
                    for (Point point1: point.toList()) {
                        if (checkIfDetectionIsValid(className) && box.contains(point1)) {
                            detectionList.add(1);
                        } else {
                            detectionList.add(0);
                        }
                    }
                    detectionCounter++;
                    if (detectionCounter == 40) {
                        if (point.toList().size() != 0) {
                            int sum = detectionList.stream().mapToInt(Integer::intValue).sum() / point.toList().size();
                            if (sum / 40f > 0.6) {
                                val levelCrossingAddress = this.dataService.levelCrossingIps.get(UUID.fromString(id));
                                if (levelCrossingAddress != null) {
                                    ChangeMotorDirectionDTO changeMotorDirectionDTO = new ChangeMotorDirectionDTO(id, 1);
                                    dataService.controlMotors(changeMotorDirectionDTO);
                                    System.out.printf("Object detected %s with confidence %s / 40.", className, sum);
                                }

                            }
                        }
                    }
                }
                Imgproc.putText(frame, cocoNames.get(classID), box.tl(), Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(255, 255, 0), 2);
                Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);
            }
        }
        return frame;
    }

    public static boolean checkIfDetectionIsValid(String className) {
        return className.equals("car")
                || className.equals("motorbike")
                || className.equals("aeroplane")
                || className.equals("bus")
                || className.equals("truck")
                || className.equals("boat");
    }

    public static boolean polygonsContainsWithDetections(MatOfPoint src1, Rect2d src2) {
        if (Imgproc.pointPolygonTest(new MatOfPoint2f(src1.toArray()), new Point(src2.tl().x, src2.tl().y), true) > 0) {
            return true;
        }
        if (Imgproc.pointPolygonTest(new MatOfPoint2f(src1.toArray()), new Point(src2.br().x, src2.br().y), true) > 0) {
            return true;
        }
        if (Imgproc.pointPolygonTest(new MatOfPoint2f(src1.toArray()), new Point(src2.br().x, src2.tl().y), true) > 0) {
            return true;
        }
        return Imgproc.pointPolygonTest(new MatOfPoint2f(src1.toArray()), new Point(src2.tl().x, src2.br().y), true) > 0;
    }
}
