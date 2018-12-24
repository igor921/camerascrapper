package igor.ryadinskii.camera.scrapper.camera;

import com.sun.jna.NativeLibrary;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

@EnableScheduling
@SpringBootApplication
public class CameraApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(CameraApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\Program Files (x86)\\VideoLAN\\VLC\\");
        MediaPlayerFactory factory = new MediaPlayerFactory();
        MediaPlayer player = factory.newHeadlessMediaPlayer();

        String mrl = "rtsp://admin:gd20160404@194.28.183.81:554";
        String options = ":sout=#transcode{vcodec=h264,venc=x264{cfr=16},scale=1,acodec=mp4a,ab=160,channels=2,samplerate=44100}:file{dst=D:/test.mp4}";

        player.playMedia(mrl, options);

        Thread.sleep(20000); // <--- sleep for 20 seconds, you should use events - but this is just a demo

        player.stop();

        player.release();
        factory.release();
    }
}

