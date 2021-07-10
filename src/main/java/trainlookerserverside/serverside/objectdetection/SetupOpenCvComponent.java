package trainlookerserverside.serverside.objectdetection;

import nu.pattern.OpenCV;
import org.apache.commons.io.FileUtils;
import org.opencv.core.Core;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import trainlookerserverside.serverside.DataService;

import java.io.File;
import java.io.IOException;

@Component
public class SetupOpenCvComponent implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private DataService dataService;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        deleteAllTmpFiles();
        setupOpenCv();
    }


    private void deleteAllTmpFiles() {
        try {
            File file = new File("videos");
            if (file.exists()) {
                FileUtils.cleanDirectory(file);
            }
        } catch (IOException ignored) { }
    }

    private void setupOpenCv() {
        String osName = System.getProperty("os.name");
        String opencvpath = System.getProperty("user.dir");
        if (osName.startsWith("Windows")) {
            int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
            if (bitness == 32) {
                opencvpath = opencvpath + "\\opencv\\build\\java\\x86\\";
            } else if (bitness == 64) {
                opencvpath = opencvpath + "\\opencv\\build\\java\\x64\\";
            } else {
                opencvpath = opencvpath + "\\opencv\\build\\java\\x86\\";
            }
        } else if (osName.equals("Mac OS X")) {
            opencvpath = opencvpath + "Your path to .dylib";
        }
        System.load(opencvpath + Core.NATIVE_LIBRARY_NAME + ".dll");
        OpenCV.loadLocally();
    }
}
