package trainlookerserverside.serverside.objectdetection;

import lombok.SneakyThrows;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class SetupOpenCvComponent implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private DataService dataService;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        setupOpenCv();
        deleteOldVideos(14);
    }

    @SneakyThrows
    public boolean isEmpty(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            }
        }
        return false;
    }

    private void deleteOldVideos(long filesRemovePeriod) {
        long DAY_IN_MS = 1000 * 60 * 60 * 24;
        Date date = new Date(System.currentTimeMillis() - (filesRemovePeriod * DAY_IN_MS));
        SimpleDateFormat monthDate = new SimpleDateFormat("yyyy/MM");
        String formattedMonthDate = monthDate.format(date);
        try {
            Files.walk(Paths.get("videos/" + formattedMonthDate)).filter(file -> {
                String fileName = file + "";
                String[] splitPath = fileName.split(Pattern.quote(File.separator));
                if (splitPath.length == 4) {
                    int dayFromFile = Integer.parseInt(splitPath[3]);
                    int daysBefore = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth();
                    return dayFromFile < daysBefore;
                }
                return false;
            }).forEach(file -> {
                try {
                    FileUtils.cleanDirectory(file.toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (isEmpty(file)) {
                    file.toFile().delete();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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
