package igor.ryadinskii.camera.scrapper.camera.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class ScrapperSevice {

    @Value("${video-data-path}")
    private String dataPath;

    @Value("${script}")
    private String ffmpegScript;

    private Process process;

    private LocalDate lastScriptRun;

    @Async
    @Scheduled(fixedDelay = 800)
    public void createDirectory(){
            LocalDateTime ldt = LocalDateTime.now();
            DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
            String formatter = formmat1.format(ldt);

            new File(String.format("%s/%s", dataPath, formatter )).mkdirs();

            if(lastScriptRun != null && DAYS.between(LocalDate.now(), lastScriptRun) != 0){
                if(process != null){
                    process.destroy();
                    createJob();
                }
            }
    }

    @Async
    @Scheduled(fixedDelay = 10000)
    public void createJob(){
            if(process == null || !process.isAlive()) {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command(generateCommands());
                builder.redirectErrorStream(true);
                try {
                    process = builder.start();
                    lastScriptRun = LocalDate.now();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    @Async
    @Scheduled(fixedDelay = 30000)
    public void deleteOldDirectories() {
        List<File> directories = Arrays.asList(Objects.requireNonNull(new File(dataPath).listFiles(File::isDirectory)));
        directories.forEach(directory -> {
            LocalDate localDate = LocalDate.parse(directory.getName());
            if(Math.abs(DAYS.between(LocalDate.now(), localDate)) >= 14)
                delete(directory);
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
        }
    }

    @PreDestroy
    public void destroy(){
        if(process != null)
            process.destroy();
    }

    private List<String> generateCommands() {
        String[] arrayCommands = ffmpegScript.split(" ");
        return Arrays.asList(arrayCommands);
    }
}
