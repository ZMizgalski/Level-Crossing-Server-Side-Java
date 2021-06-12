package trainlookerserverside.serverside;

import lombok.SneakyThrows;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import trainlookerserverside.serverside.objectdetection.ObjectDetectionService;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class ServersideApplication {


    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(ServersideApplication.class, args);
        ObjectDetectionService.runDetection();
    }
//
//    public static BufferedImage Mat2BufferedImage(Mat m) {
//        int type = BufferedImage.TYPE_BYTE_GRAY;
//        if (m.channels() > 1) {
//            type = BufferedImage.TYPE_3BYTE_BGR;
//        }
//        int bufferSize = m.channels() * m.cols() * m.rows();
//        byte[] b = new byte[bufferSize];
//        m.get(0, 0, b);
//        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
//        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
//        System.arraycopy(b, 0, targetPixels, 0, b.length);
//        return image;
//    }

//    @SneakyThrows
//    private static void runDetection() {
//
//        // Camera settings
//        Mat frame = new Mat();
//        VideoCapture camera = new VideoCapture(0);
//        double width = camera.get(Videoio.CAP_PROP_FRAME_WIDTH);
//        double height = camera.get(Videoio.CAP_PROP_FRAME_HEIGHT);
//
//        // Detect Thresh
//        double confThreshold = 0.15;
//        double maxThreshold = 0.2;
//
//        // Obejct Names
//        List<String> detectedObjectsNames = new ArrayList<>();
//        File detectedObjectsFile = new ClassPathResource("deepLerningModels/coco.names").getFile();
//        Scanner myReader = new Scanner(detectedObjectsFile);
//        while (myReader.hasNextLine()) {
//            String data = myReader.nextLine();
//            detectedObjectsNames.add(data);
//        }
//        myReader.close();
//
//        // AI Setup
//        File modelAIConfigFile = new ClassPathResource("deepLerningModels/yolov3-tiny.cfg").getFile();
//        File modelAIWeights = new ClassPathResource("deepLerningModels/yolov3-tiny.weights").getFile();
//
//        Net net = Dnn.readNetFromDarknet(modelAIConfigFile.getAbsolutePath(), modelAIWeights.getAbsolutePath());
//        net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
//        net.setPreferableTarget(Dnn.DNN_TARGET_CPU);
//
//        // Test UI setup
//        JFrame jframe = new JFrame("Testing");
//        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        JLabel vidPanel = new JLabel();
//        jframe.setContentPane(vidPanel);
//        jframe.setSize((int) width, (int) height);
//        jframe.setVisible(true);
//
//        while (true) {
//            if (camera.read(frame)) {
//                ImageIcon image = new ImageIcon(Mat2BufferedImage(onCameraFrame(frame, net)));
//                vidPanel.setIcon(image);
//                vidPanel.repaint();
//            }
//        }
//    }

//    public static Mat onCameraFrame(Mat inputFrame, Net tinyYolo) {
//        Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGBA2RGB);
//        Mat imageBlob = Dnn.blobFromImage(inputFrame, 0.00392, new Size(416, 416), new Scalar(0, 0, 0),/*swapRB*/false, /*crop*/false);
//        tinyYolo.setInput(imageBlob);
//        java.util.List<Mat> result = new java.util.ArrayList<Mat>(2);
//        List<String> outBlobNames = new java.util.ArrayList<>();
//        outBlobNames.add(0, "yolo_16");
//        outBlobNames.add(1, "yolo_23");
//        tinyYolo.forward(result, outBlobNames);
//        float confThreshold = 0.3f;
//        List<Integer> clsIds = new ArrayList<>();
//        List<Float> confs = new ArrayList<>();
//        List<Rect2d> rects = new ArrayList<>();
//        for (Mat level : result) {
//            for (int j = 0; j < level.rows(); ++j) {
//                Mat row = level.row(j);
//                Mat scores = row.colRange(5, level.cols());
//                Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
//                float confidence = (float) mm.maxVal;
//                Point classIdPoint = mm.maxLoc;
//                if (confidence > confThreshold) {
//                    int centerX = (int) (row.get(0, 0)[0] * inputFrame.cols());
//                    int centerY = (int) (row.get(0, 1)[0] * inputFrame.rows());
//                    int width = (int) (row.get(0, 2)[0] * inputFrame.cols());
//                    int height = (int) (row.get(0, 3)[0] * inputFrame.rows());
//                    int left = centerX - width / 2;
//                    int top = centerY - height / 2;
//                    clsIds.add((int) classIdPoint.x);
//                    confs.add(confidence);
//                    rects.add(new Rect2d(left, top, width, height));
//                }
//            }
//        }
//        int ArrayLength = confs.size();
//        if (ArrayLength >= 1) {
//            float nmsThresh = 0.2f;
//            MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));
//
//            Rect2d[] boxesArray = rects.toArray(new Rect2d[0]);
//            MatOfRect2d matOfRect = new MatOfRect2d(boxesArray);
//            MatOfInt indices = new MatOfInt();
//            Dnn.NMSBoxes(matOfRect, confidences, confThreshold, nmsThresh, indices);
//            int[] ind = indices.toArray();
//            for (int idx : ind) {
//                Rect2d box = boxesArray[idx];
//                int idGuy = clsIds.get(idx);
//                float conf = confs.get(idx);
//                List<String> cocoNames = Arrays.asList("a person", "a bicycle", "a motorbike", "an airplane", "a bus", "a train", "a truck", "a boat", "a traffic light", "a fire hydrant", "a stop sign", "a parking meter", "a car", "a bench", "a bird", "a cat", "a dog", "a horse", "a sheep", "a cow", "an elephant", "a bear", "a zebra", "a giraffe", "a backpack", "an umbrella", "a handbag", "a tie", "a suitcase", "a frisbee", "skis", "a snowboard", "a sports ball", "a kite", "a baseball bat", "a baseball glove", "a skateboard", "a surfboard", "a tennis racket", "a bottle", "a wine glass", "a cup", "a fork", "a knife", "a spoon", "a bowl", "a banana", "an apple", "a sandwich", "an orange", "broccoli", "a carrot", "a hot dog", "a pizza", "a doughnut", "a cake", "a chair", "a sofa", "a potted plant", "a bed", "a dining table", "a toilet", "a TV monitor", "a laptop", "a computer mouse", "a remote control", "a keyboard", "a cell phone", "a microwave", "an oven", "a toaster", "a sink", "a refrigerator", "a book", "a clock", "a vase", "a pair of scissors", "a teddy bear", "a hair drier", "a toothbrush");
//                int intConf = (int) (conf * 100);
//                Imgproc.putText(inputFrame, cocoNames.get(idGuy) + " " + intConf + "%", box.tl(), Imgproc.FONT_HERSHEY_COMPLEX, 2, new Scalar(255, 255, 0), 2);
//                Imgproc.rectangle(inputFrame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);
//            }
//        }
//        return inputFrame;
//    }
}
