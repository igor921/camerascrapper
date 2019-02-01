package igor.ryadinskii.camera.scrapper.camera.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class ScrapperSevice {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final ExecutorService executor = Executors.newCachedThreadPool();

    @Value("${video-data-path}")
    private String dataPath;

    @Value("${camera-url}")
    private String cameraUrl;

    @Value("${script}")
    private String ffmpegScript;


    @Value("${mail-to}")
    private String mailTo;

    @Autowired
    private JavaMailSender emailSender;

    @Async
    @Scheduled(fixedDelay = 800)
    public void createDirectory() {
        checkDiskSpace();
        String formatter = getDateTimeInFormat("yyyy-MM-dd");

        new File(String.format("%s/%s", dataPath, formatter)).mkdirs();
        new File(String.format("%s/%s/%s", dataPath, formatter, "moving")).mkdirs();
    }

    @Async
    @Scheduled(fixedDelay = 30000)
    public void deleteOldDirectories() {
        List<File> directories = Arrays.asList(Objects.requireNonNull(new File(dataPath).listFiles(File::isDirectory)));
        directories.forEach(directory -> {
            try {
                LocalDate localDate = LocalDate.parse(directory.getName());
                if (Math.abs(DAYS.between(LocalDate.now(), localDate)) >= 7)
                    delete(directory);
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        });
    }

    @Async
    @Scheduled(fixedDelay = 300000)
    public void detectMove() {

        executor.submit(()->{
            try{
                String formatter = getDateTimeInFormat("yyyy-MM-dd");

                List<File> directories = Arrays.asList(Objects.requireNonNull(new File(String.format("%s/%s",dataPath,formatter)).listFiles()));
                if(directories != null && directories.size() > 1){
                    File file = directories.get(directories.size() - 2);
                    if(MovingDetection.detectMove(file)){

                            Files.copy(file.toPath(),
                                    new File(String.format("%s/%s/%s/%s", dataPath, formatter, "moving", file.getName())).toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);

                    }
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }



    private String getDateTimeInFormat(String format) {
        LocalDateTime ldt = LocalDateTime.now();
        DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern(format, Locale.ENGLISH);
        return formmat1.format(ldt);
    }

    private void delete(File f) {
        try {
            if (f.isDirectory()) {
                for (File c : f.listFiles())
                    delete(c);
            }
            f.delete();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
        }
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private void checkDiskSpace() {
        double result;
        long bytes = new File("/").getFreeSpace();
        boolean si = true;
        int unit = si ? 1000 : 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        result = bytes / Math.pow(unit, exp);

        if (result < 3) {
            sendSimpleMessage(mailTo, "Low disk space", "Only " + result + "GB left");
        }

    }
}
