package igor.ryadinskii.camera.scrapper.camera.services;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@Component
public class MovingDetection {

    static {
        //https://docs.opencv.org/3.4.5/d2/de6/tutorial_py_setup_in_ubuntu.html
        //load library
        System.load("/usr/local/share/OpenCV/java/libopencv_java345.so");
        /*
        sudo apt-get install libv4l-dev
cmake -DWITH_LIBV4L=ON .. (or similar, it's important to have the WITH_LIBV4L enabled
make && sudo make install

         */
        //sudo apt-get install libavcodec-dev libavformat-dev libavdevice-dev
    }

    public static boolean detectMove(File file){

        VideoCapture capture = new VideoCapture(file.getPath());

        int countersss = 0;
        int counterBelow57 = 0;

        try {
            BufferedImage prevFile = null;
            int counter = 0;
            while (capture.isOpened()) {
                Mat frame = new Mat();
                capture.read(frame);

                if (counter < 3 && counter != 0) {
                    counter++;
                    continue;
                } else counter = 0;


                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);

                MatOfByte buffer = new MatOfByte();
                Imgcodecs.imencode(".png", frame, buffer);
                frame.release();

                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(buffer.toArray()));

                    counter++;

                    if (prevFile == null) {
                        prevFile = img;
                        continue;
                    }

                    float percent = compareImage(prevFile, img);
                    prevFile = img;

                    if(percent < 40 || countersss > 110 || counterBelow57 > 90){
                        return true;
                    }

                    if (percent < 90) {
                        countersss++;
                    }

                    if(percent < 57)
                        counterBelow57++;

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                }
            }

        } catch (Exception ex) {
            return false;
        } finally {
            capture.release();
        }

        return false;

    }

    private static float compareImage(BufferedImage biA, BufferedImage biB) {

        float percentage = 0;
        try {
            // take buffer data from both image files //
            //BufferedImage biA = ImageIO.read(fileA);
            DataBuffer dbA = biA.getData().getDataBuffer();
            int sizeA = dbA.getSize();
            // BufferedImage biB = ImageIO.read(fileB);
            DataBuffer dbB = biB.getData().getDataBuffer();
            int sizeB = dbB.getSize();
            int count = 0;
            // compare data-buffer objects //
            if (sizeA == sizeB) {

                for (int i = 0; i < sizeA; i++) {

                    if (Math.abs(dbA.getElem(i) - dbB.getElem(i)) < 2) {
                        count = count + 1;
                    }
                }
                percentage = (count * 100) / sizeA;
            } else {
                System.out.println("Both the images are not of same size");
            }

        } catch (Exception e) {
            System.out.println("Failed to compare image files ...");
        }
        return percentage;
    }

}
