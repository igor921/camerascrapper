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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class ScrapperSevice {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final ExecutorService cameraHolderExecutor = Executors.newCachedThreadPool();

    @Value("${video-data-path}")
    private String dataPath;

    @Value("${camera-url}")
    private String cameraUrl;

    @Value("${script}")
    private String ffmpegScript;

    @Autowired
    public JavaMailSender emailSender;

    private ProcessHolder processHolder;

    private LocalDate lastScriptRun;

    private String mailTo = "igor.ryadinskii@gmail.com";

    private boolean first = true;

    @Async
    @Scheduled(fixedDelay = 800)
    public void createDirectory(){
        if(first){
            first = false;
            sendSimpleMessage(mailTo, "Test", "test");
        }
            checkDiskSpace();
            LocalDateTime ldt = LocalDateTime.now();
            DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
            String formatter = formmat1.format(ldt);

            new File(String.format("%s/%s", dataPath, formatter )).mkdirs();

            if(lastScriptRun != null && DAYS.between(LocalDate.now(), lastScriptRun) != 0){
                if(processHolder != null){
                   processHolder.killProcess();
                    createJob();
                }
            }
    }

    private void checkDiskSpace() {
        double result;
        long bytes = new File("/").getFreeSpace();
        boolean si = true;
        int unit = si ? 1000 : 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        result = bytes / Math.pow(unit, exp);

        if(result < 3){
            sendSimpleMessage(mailTo, "Low disk space", "Only " + result + "GB left");
        }

    }

    @Async
    @Scheduled(fixedDelay = 10000)
    public void checkCameraEnabled(){
        try {
        URL url = new URL(cameraUrl);
        HttpURLConnection con = null;
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(false);
        con.setConnectTimeout(4000);
        con.getResponseCode();
        } catch (Exception e) {
            if(processHolder != null)
                processHolder.killProcess();
            e.printStackTrace();
        }
    }

    @Async
    @Scheduled(fixedDelay = 10000)
    public void createJob(){
            if(processHolder == null || !processHolder.isAlive()) {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command(generateCommands());
                builder.redirectErrorStream(true);
                try {
                    Process process = builder.start();
                    lastScriptRun = LocalDate.now();
                    processHolder = new ProcessHolder(process);
                    cameraHolderExecutor.submit(processHolder);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    e.printStackTrace();
                }
            }
    }



    @Async
    @Scheduled(fixedDelay = 30000)
    public void deleteOldDirectories() {
        List<File> directories = Arrays.asList(Objects.requireNonNull(new File(dataPath).listFiles(File::isDirectory)));
        directories.forEach(directory -> {
            try {
                LocalDate localDate = LocalDate.parse(directory.getName());
                if (Math.abs(DAYS.between(LocalDate.now(), localDate)) >= 14)
                    delete(directory);
            } catch (Exception ex){
                logger.error(ex.getMessage());
            }
        });
    }

    private void delete(File f) {
        try {
            if (f.isDirectory()) {
                for (File c : f.listFiles())
                    delete(c);
            }
            f.delete();
        }catch (Exception ex){
            ex.printStackTrace();
            logger.error(ex.getMessage());
        }
    }

    @PreDestroy
    public void destroy(){
        if(processHolder != null)
            processHolder.killProcess();
    }

    private List<String> generateCommands() {
        String[] arrayCommands = ffmpegScript.split(" ");
        return Arrays.asList(arrayCommands);
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
        } catch (Exception ex){
            logger.error(ex.getMessage());
        }
    }
}
