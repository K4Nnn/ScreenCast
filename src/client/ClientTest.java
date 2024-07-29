package client;

import java.net.*;
import java.io.*;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.event.*;
import cryptoUtils.*;
import javaGUI.*;

public class ClientTest {

   private static Socket client = null;
   private static String serverName;
   private static int port;
   private static String in_username;
   private static String in_password;
   private static ScreenCaptureSender screenCaptureSender;
   // private static boolean loginButtonClicked = false;

   public static void main(String[] args) {
      serverName = args[0];
      port = Integer.parseInt(args[1]);

      // 加密工具
      cryptoAES aesUtil = new cryptoAES();
      String AESKey = cryptoAES.AESKeyGen();
      byte[] AESiv = cryptoAES.generateIV();
      aesUtil.setKey(AESKey);
      aesUtil.setIV(AESiv);

      LoginPanel loginPanel = new LoginPanel();
      // 创建一个面板
      JFrame loginFrame = createFrame("Login", 480, 360);
      loginPanel.setSwitchButtonListener(new ActionListener() {
         @Override
            public void actionPerformed(ActionEvent e) {
               try{
                  runRegister(client, aesUtil);
               } catch( Exception ex ){
                  ex.printStackTrace();
               }
         }
      });
      loginFrame.getContentPane().add(loginPanel);

      boolean isLoginPassed = false;
      boolean loginButtonClicked = false;
      while( !isLoginPassed ){
         // 当用户还未点击登录按钮时，阻塞程序
         while ( !loginButtonClicked ) {
            loginButtonClicked = loginPanel.getLoginClicked();
            try {
               Thread.sleep(100); // Sleep to reduce CPU usage
            } catch (InterruptedException e) {
               e.printStackTrace();
            } catch (Exception e){
               e.printStackTrace();
            }
         }
         // 准备由client向server传送数据
         try{
            client = connectToServer(serverName, port);
            verifyServer(client, aesUtil);
            sendPreMessage("login", aesUtil);
            in_username = loginPanel.getUsername();
            in_password = loginPanel.getPassword();
            isLoginPassed = startClient(in_username, in_password, client, aesUtil);
            if( !isLoginPassed ){
               // 弹窗：重新登录
               JOptionPane.showMessageDialog(loginPanel, "Wrong Username or Password! Please try again.", 
                     "Login Failed", JOptionPane.WARNING_MESSAGE);
               loginButtonClicked = false;
            }
            loginPanel.setLoginClicked(false);
         } catch( IOException e ){
            e.printStackTrace();
         } catch( Exception e){
            e.printStackTrace();
         }
      }
      // 根据server返回结果判断是否转向新的界面
      // 关闭原先的login界面
      loginFrame.dispose();
      // 创建新的client主界面
      JFrame mainClientFrame = createFrame("ScreenCast", 300, 240);
      MainClientPanel mainClientPanel = new MainClientPanel();
      mainClientFrame.add(mainClientPanel);
      // Terminate按钮：用于关闭程序
      mainClientPanel.setTerminateListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
             mainClientFrame.dispose();
             try {
                 client.close();
             } catch (IOException ex) {
                 ex.printStackTrace();
             }
         }
      });
      // 启动屏幕捕捉和传输线程
      screenCaptureSender = new ScreenCaptureSender(client);
      screenCaptureSender.start();
      
   }

   private static JFrame createFrame(String name, int frame_width, int frame_height){
      JFrame frame = new JFrame(name);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setSize(frame_width, frame_height);
      frame.setVisible(true);
      return frame;
   }

   private static void sendPreMessage(String preMessage, cryptoAES aesUtil) throws IOException, Exception{
      OutputStream outToServer = client.getOutputStream();
      DataOutputStream out = new DataOutputStream(outToServer);
      String typeOfMsg = null;
      if( preMessage.equals("login"))
         typeOfMsg = aesUtil.encryptAES("1");
      else if( preMessage.equals("register"))
         typeOfMsg = aesUtil.encryptAES("2");
      out.writeUTF(typeOfMsg);
      System.out.println("Send premessage to server.");
   }

   private static void runRegister(Socket client, cryptoAES aesUtil) throws IOException, Exception{
      RegisterPanel registerPanel = new RegisterPanel();
      JFrame registerFrame = createFrame("Register", 480, 360);
      registerFrame.add(registerPanel);
      boolean registerPassed = false;
      while( !registerPassed ){
         // 如果没点击button，阻塞
         while( !registerPanel.getRegisterClicked() ){
            try{
               Thread.sleep(100);
            } catch ( InterruptedException e){
               e.printStackTrace();
            }
         };
         // 点击button后进行处理逻辑
         client = connectToServer(serverName, port);
         verifyServer(client, aesUtil);
         sendPreMessage("register", aesUtil);
         String input_username = aesUtil.encryptAES(registerPanel.getName());
         String input_password = aesUtil.encryptAES(registerPanel.getPassword());
         OutputStream outToServer = client.getOutputStream();
         DataOutputStream out = new DataOutputStream(outToServer);
         out.writeUTF(input_username);
         out.writeUTF(input_password);
         System.out.println("Sent encrypted register message to Server.");
         InputStream inFromServer = client.getInputStream();
         DataInputStream in = new DataInputStream(inFromServer);
         // 收到返回信息，解密
         String receivedReturn = aesUtil.decryptAES(in.readUTF());
         if( receivedReturn.equals("250")){
            JOptionPane.showMessageDialog(registerPanel, "Successfully Register!", 
                     "Register", JOptionPane.INFORMATION_MESSAGE);
            registerPassed = true;
            registerFrame.dispose();
         } else if( receivedReturn.equals("401") ){
            JOptionPane.showMessageDialog(registerPanel, "Some error emerged. Please try again.", 
                     "Register Failed", JOptionPane.WARNING_MESSAGE);
         }
         client.close();
         client = null;
      }
   }

   private static void verifyServer(Socket client, cryptoAES aesUtil) throws Exception{
      // 接收Server的RSA公钥
      String serverPublicKey = receiveServerPublicKey(client);
      savePublicKeyToFile(serverPublicKey);

      // 加密由Client生成的AES会话密钥，发送至Server
      String encData = encryptAESKeyWithRSA(serverPublicKey, aesUtil.getKey());
      int nonce = generateNonce();
      sendEncryptedData(client, encData, nonce, aesUtil.getIV());

      // 接收Server加密的nonce，解密后验证合法性
      verifyServerResponse(client, aesUtil, nonce);
   }

   private static boolean startClient(String input_username, String input_password, Socket client, cryptoAES aesUtil){
      try {
         // 接收client程序login输入，传送至server
         // 程序默认无明文存储，usr和pwd应至少以base64存储、传输
         String in_username = Base64.getEncoder().encodeToString(input_username.getBytes());
         String in_password = Base64.getEncoder().encodeToString(input_password.getBytes());
         in_username = encryptDataByAES(in_username, aesUtil);
         in_password = encryptDataByAES(in_password, aesUtil);
         sendLoginInput(client, in_username, in_password);
         System.out.println("------Initial Transmition Done.------");
         // 接收server对login信息的反馈，信息都以base64存储
         String loginReMsg = receiveLoginVerification(client, aesUtil);
         return loginReMsg.equals(Base64.getEncoder().encodeToString("Passed".getBytes()));
      } catch (IOException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      } 
      return false;
   }

   private static Socket connectToServer(String serverName, int port) throws IOException {
      System.out.println("Connect to host: " + serverName + " , port: " + port);
      Socket client = new Socket(serverName, port);
      System.out.println("The host address: " + client.getRemoteSocketAddress());
      return client;
   }

   private static String receiveServerPublicKey(Socket client) throws IOException {
      InputStream inFromServer = client.getInputStream();
      DataInputStream in = new DataInputStream(inFromServer);
      String serverPublicKey = in.readUTF();
      System.out.println("Received Server Public Key: " + serverPublicKey);
      return serverPublicKey;
   }

   private static void savePublicKeyToFile(String serverPublicKey) throws IOException {
      File keyFile = new File("src\\client\\rsa_pubkey_from_server.txt");
      if (!keyFile.exists()) {
         keyFile.createNewFile();
      }
      FileWriter fw = new FileWriter(keyFile.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(serverPublicKey);
      bw.close();
   }

   private static String encryptAESKeyWithRSA(String serverPublicKey, String AESKey) throws Exception {
      String encData = cryptoRSA.encryptRSA(cryptoRSA.ALGORITHM, AESKey,
            (Key) cryptoRSA.loadPublicKeyFromString(cryptoRSA.ALGORITHM, serverPublicKey), cryptoRSA.MAX_ENCRYPT_SIZE);
      System.out.println("Encrypted AES Key: " + encData);
      return encData;
   }

   // 随机生成32位的nonce（4字节）
   private static int generateNonce() {
      SecureRandom randomFigure = new SecureRandom();
      byte[] keyBytes = new byte[4];
      randomFigure.nextBytes(keyBytes);
      int nonce = (keyBytes[0] & 0xff) | ((keyBytes[1] << 8) & 0xff00) | ((keyBytes[2] << 24) >>> 8) | (keyBytes[3] << 24);
      System.out.println("Generated Nonce: " + nonce);
      return nonce;
   }

   private static void sendEncryptedData(Socket client, String encData, int nonce, byte[] AESiv) throws IOException {
      OutputStream outToServer = client.getOutputStream();
      DataOutputStream out = new DataOutputStream(outToServer);
      // 使用‘\n’进行数据分隔
      encData += "\n" + nonce;
      out.writeUTF(encData);
      out.write(AESiv);
      System.out.println("Sent Encrypted Data and IV to Server.");
   }

   // 验证nonce合法性
   // 缺少步骤：nonce的加密解密
   private static void verifyServerResponse(Socket client, cryptoAES aesUtil, int nonce) throws Exception {
      InputStream inFromServer = client.getInputStream();
      DataInputStream in = new DataInputStream(inFromServer);

      String verifyNonce = in.readUTF();
      System.out.println("The received data: " + verifyNonce);
      // 拿AES会话密钥解密
      verifyNonce = aesUtil.decryptAES(verifyNonce);
      System.out.println("The received Base64 nonce: " + verifyNonce);
      // Base64逆编码
      verifyNonce = new String(Base64.getDecoder().decode(verifyNonce));
      System.out.println("The server's nonce: "+verifyNonce);
      int verifyNonceInt = Integer.parseInt(verifyNonce);
      if (verifyNonceInt == nonce + 1)
         System.out.println("Receive from Server: the message:  " + verifyNonceInt + ". Verify passed.");
      else {
         System.out.println("Verify failed. Exit.");
         client.close();
         System.exit(1); // 终止程序
      }
   }

   private static void sendLoginInput(Socket client, String encUsr, String encPwd) throws IOException {
      OutputStream outToServer = client.getOutputStream();
      DataOutputStream out = new DataOutputStream(outToServer);
      out.writeUTF(encUsr);
      out.writeUTF(encPwd);
      System.out.println("Sent encrypted login message to Server.");
   }

   private static String receiveLoginVerification(Socket client, cryptoAES aesUtil) throws IOException, Exception{
      InputStream inFromServer = client.getInputStream();
      DataInputStream in = new DataInputStream(inFromServer);
      String result = aesUtil.decryptAES(in.readUTF());
      return result;
   }

   // 使用AES会话密钥加密Login数据
   private static String encryptDataByAES(String data, cryptoAES aesUtil) throws Exception{
      return aesUtil.encryptAES(data);
   }
}
