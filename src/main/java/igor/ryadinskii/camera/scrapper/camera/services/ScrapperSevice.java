package igor.ryadinskii.camera.scrapper.camera.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ScrapperSevice {

    @Value("${video-data-path}")
    private String dataPath;

    @Value("${script}")
    private String ffmpegScript;

    private Process process;

    @Async
    @Scheduled(fixedDelay = 800)
    public void createDirectory(){
            LocalDateTime ldt = LocalDateTime.now();
            DateTimeFormatter formmat1 = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH);
            String formatter = formmat1.format(ldt);

            new File(String.format("%s/%s", dataPath, formatter )).mkdirs();
    }

    @Async
    @Scheduled(fixedDelay = 10000)
    public void createJob(){
            if(process == null || !process.isAlive()) {
                ProcessBuilder builder = new ProcessBuilder();
                builder.command(ffmpegScript);
                builder.redirectErrorStream(true);
                try {
                    process = builder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }
}
