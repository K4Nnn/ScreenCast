package client;

import com.sun.jna.platform.win32.GDI32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

import cryptoUtils.cryptoAES;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;

public class ScreenCaptureSender extends Thread {
    private Socket client;
    private volatile boolean running = true;
    private volatile int frequency = 24; // 使用volatile确保线程间变量可见性
    private Timer timer;

    public ScreenCaptureSender(Socket client) {
        this.client = client;
        this.timer = new Timer();
        scheduleCaptureTask();
        new Thread(new ControlListener(client)).start(); // 启动监听线程
    }

    public void terminate() {
        running = false;
        timer.cancel();
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
        timer.cancel(); // 取消当前定时任务
        timer = new Timer(); // 创建新的定时器
        scheduleCaptureTask(); // 重新调度定时任务
    }

    private void scheduleCaptureTask() {
        long period = 1000 / frequency;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (running && !client.isClosed()) {
                    captureAndSendFrame();
                }
            }
        }, 0, period);
    }

    private void captureAndSendFrame() {
        try {
            OutputStream os = client.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            User32 user32 = User32.INSTANCE;
            HWND hwnd = user32.GetDesktopWindow();
            BufferedImage screenCapture = GDI32Util.getScreenshot(hwnd);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenCapture, "png", baos); // 使用无损格式PNG
            byte[] imageBytes = baos.toByteArray();

            if (!client.isClosed()) {
                // 先发送图像大小
                dos.writeInt(imageBytes.length);
                // 再发送图像数据
                dos.write(imageBytes);
                dos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 监听来自服务端的消息以控制频率
    private class ControlListener implements Runnable {
        private Socket client;
        private cryptoAES aesUtil;

        public ControlListener(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                InputStream is = client.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String message;
                while ((message = aesUtil.decryptAES(br.readLine())) != null) {
                    if (message.startsWith("FREQ:")) {
                        int newFrequency = Integer.parseInt(message.substring(5).trim());
                        setFrequency(newFrequency);
                        System.out.println("Frequency set to: " + newFrequency);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
