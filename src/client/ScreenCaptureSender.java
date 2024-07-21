package client;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;

public class ScreenCaptureSender extends Thread {
    private Socket client;
    private volatile boolean running = true;

    public ScreenCaptureSender(Socket client) {
        this.client = client;
    }

    public void terminate() {
        running = false;
    }

    @Override
    public void run() {
        try {
            Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRect = new Rectangle(screenSize);
            OutputStream os = client.getOutputStream();

            while (running) {
                BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(screenCapture, "jpeg", baos);
                byte[] imageBytes = baos.toByteArray();
                os.write(imageBytes);
                os.flush();
                Thread.sleep(1000 / 24); // 控制帧率为60帧每秒
            }
        } catch (IOException | AWTException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}