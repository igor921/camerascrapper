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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Async
    @Scheduled(fixedDelay = 800)
    public void createDirectory(){
            checkDiskSpace();
            String formatter = getDateTimeInFormat("yyyy-MM-dd");

            new File(String.format("%s/%s", dataPath, formatter )).mkdirs();

            /*if(lastScriptRun != null && DAYS.between(LocalDate.now(), lastScriptRun) != 0){
                if(processHolder != null){
                   processHolder.killProcess();
                   createJob();
                }
            }*/
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

    /*@Async
    @Scheduled(fixedDelay = 10000)
    public void checkCameraEnabled() {
        try {
            URL url = new URL(cameraUrl);
            HttpURLConnection con = null;
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(false);
            con.setConnectTimeout(4000);
            con.getResponseCode();
        } catch (Exception e) {
            if (processHolder != null)
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

*/

    @Async
    @Scheduled(fixedDelay = 30000)
    public void deleteOldDirectories() {
        List<File> directories = Arrays.asList(Objects.requireNonNull(new File(dataPath).listFiles(File::isDirectory)));
        directories.forEach(directory -> {
            try {
                LocalDate localDate = LocalDate.parse(directory.getName());
                if (Math.abs(DAYS.between(LocalDate.now(), localDate)) >= 4)
                    delete(directory);
            } catch (Exception ex){
                logger.error(ex.getMessage());
            }
        });
    }

 /*   @Async
    @Scheduled(fixedDelay = 10000)
    public void checkFrameChanges() {

        try {
            String formatter = getDateTimeInFormat("yyyy-MM-dd");
            Optional<Path> lastFilePath = Files.list(Paths.get(dataPath + "/" + formatter))    // here we get the stream with full directory listing
                    .filter(f -> !Files.isDirectory(f))  // exclude subdirectories from listing
                    .max(Comparator.comparingLong(f -> f.toFile().lastModified()));  // finally get the last file using simple comparator by lastModified field

            if (lastFilePath.isPresent()) // your folder may be empty
            {
                Long lastModifird = Long.valueOf(lastFilePath.get().toFile().getName().replace(".mp4",""));
                LocalDateTime currentTime = LocalDateTime.now();
                Long current = Long.valueOf(String.format("%s%s%s%s%s%s",
                        currentTime.getYear(), formatDate(currentTime.getMonth().getValue()), formatDate(currentTime.getDayOfMonth()),
                        formatDate(currentTime.getHour()), formatDate(currentTime.getMinute()), formatDate(currentTime.getSecond())));
                if(Math.abs(current - lastModifird) >= 240){
                    logger.info(String.format("Last %s", lastModifird));
                    logger.info(String.format("Current %s", current));
                    logger.info("Last frame didn't change over 2 minutes");
                    processHolder.killProcess();
                    createJob();
                }
            }
        }catch (Exception ex){
            logger.error(ex.getMessage());
        }
    }
*/
    private String getDateTimeInFormat(String format){
        LocalDateTime ldt = LocalDateTime.now();
        DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern(format, Locale.ENGLISH);
        return formmat1.format(ldt);
    }

    private String formatDate(int value){
        if (value < 10)
            return "0" + value;
        return value+"";
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

    /*@PreDestroy
    public void destroy(){
        if(processHolder != null)
            processHolder.killProcess();
    }

    private List<String> generateCommands() {
        String[] arrayCommands = ffmpegScript.split(" ");
        return Arrays.asList(arrayCommands);
    }
*/
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
