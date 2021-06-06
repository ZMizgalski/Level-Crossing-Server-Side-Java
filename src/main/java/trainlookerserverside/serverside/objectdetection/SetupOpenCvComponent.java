package trainlookerserverside.serverside.objectdetection;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class SetupOpenCvComponent implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        setupOpenCv();
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
