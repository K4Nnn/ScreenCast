package client;

import com.sun.jna.platform.win32.GDI32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
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
            OutputStream os = client.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            User32 user32 = User32.INSTANCE;
            HWND hwnd = user32.GetDesktopWindow();

            while (running && !client.isClosed()) {
                BufferedImage screenCapture = GDI32Util.getScreenshot(hwnd);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(screenCapture, "png", baos); // 使用无损格式PNG
                byte[] imageBytes = baos.toByteArray();

                if( !client.isClosed() ){
                    // 先发送图像大小
                    dos.writeInt(imageBytes.length);
                    // 再发送图像数据
                    dos.write(imageBytes);
                    dos.flush();
                    Thread.sleep(1000 / 30); // 控制帧率为24帧每秒
                }
            }
            if ( client.isClosed() )
                System.out.println("The connection has already been closed. ");
            this.client = null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 下面为使用Robot类实现屏幕截图
    // @Override
    // public void run() {
    //     try {
    //         Robot robot = new Robot();
    //         Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    //         Rectangle screenRect = new Rectangle(screenSize);
    //         OutputStream os = client.getOutputStream();
    //         DataOutputStream dos = new DataOutputStream(os);

    //         while (running) {
    //             BufferedImage screenCapture = robot.createScreenCapture(screenRect);
    //             ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //             ImageIO.write(screenCapture, "png", baos); // 使用无损格式PNG
    //             byte[] imageBytes = baos.toByteArray();

    //             // 先发送图像大小
    //             dos.writeInt(imageBytes.length);
    //             // 再发送图像数据
    //             dos.write(imageBytes);
    //             dos.flush();

    //             Thread.sleep(1000 / 24); // 控制帧率为24帧每秒
    //         }
    //     } catch (IOException | AWTException | InterruptedException e) {
    //         e.printStackTrace();
    //     }
    // }
}
