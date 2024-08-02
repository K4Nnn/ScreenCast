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
   private static String statusCode;
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

      // 创建开始面板
      BeginFrame beginFrame = new BeginFrame();
      beginFrame.setVisible(true);
      LoginPanel loginPanel = beginFrame.getLoginPanel();
      RegisterPanel registerPanel = beginFrame.getRegisterPanel();

      // 监听是否点击LoginButton or SwitchButton/Register Button
      // 直到loginButton点击，并通过验证，才结束当前界面
      boolean isLoginPassed = false;
      boolean loginButtonClicked = false;
      boolean registerButtonClicked = false;
      while( !isLoginPassed ){
         // 当用户还未点击login/register按钮时，阻塞程序
         while ( !loginButtonClicked && !registerButtonClicked) {
            loginButtonClicked = loginPanel.getLoginClicked();
            registerButtonClicked = registerPanel.getRegisterClicked();
            try {
               Thread.sleep(100); // Sleep to reduce CPU usage
            } catch (InterruptedException e) {
               e.printStackTrace();
            } catch (Exception e){
               e.printStackTrace();
            }
         }
         // 判断是login/register传送数据
         if( loginButtonClicked ){
            try{
               client = connectToServer(serverName, port);
               verifyServer(client, aesUtil);
               sendPreMessage("login", aesUtil);
               in_username = loginPanel.getUsername();
               in_password = loginPanel.getPassword();
               isLoginPassed = startClient("login", in_username, in_password, client, aesUtil);
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
         }  else if( registerButtonClicked ){
            try{
               client = connectToServer(serverName, port);
               verifyServer(client, aesUtil);
               sendPreMessage("register", aesUtil);
               in_username = registerPanel.getUsername();
               in_password = registerPanel.getPassword();
               registerButtonClicked = false;
               registerPanel.setRegisterClicked(false);
               boolean isRegisterPassed = startClient("register", in_username, in_password, client, aesUtil);
               if( !isRegisterPassed ){
                  // 弹窗：注册失败
                  if( statusCode.equals("401"))
                     JOptionPane.showMessageDialog(registerPanel, "Registration failed: Username exists!", "Register Failed", JOptionPane.WARNING_MESSAGE);
                  else if( statusCode.equals("403"))
                     JOptionPane.showMessageDialog(registerPanel, "Registration failed: IP limit exceeded!", "Register Failed", JOptionPane.WARNING_MESSAGE);
                  else
                     JOptionPane.showMessageDialog(registerPanel, "Registration failed. Please check your username and password.", "Register Failed", JOptionPane.WARNING_MESSAGE);
               }else{
                  JOptionPane.showMessageDialog(registerPanel, "Successfully registered.", 
                        "Register Successfully", JOptionPane.INFORMATION_MESSAGE);
               }
            } catch( IOException e ){
               e.printStackTrace();
            } catch( Exception e){
               e.printStackTrace();
            }
         }
      }
      // 根据server返回结果判断是否转向新的界面
      // 关闭原先的开始界面
      beginFrame.dispose();
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

   private static boolean startClient(String action, String input_username, String input_password, Socket client, cryptoAES aesUtil){
      // 接收client程序login输入，传送至server
      // 程序默认无明文存储，usr和pwd应至少以base64存储、传输
      String in_username = Base64.getEncoder().encodeToString(input_username.getBytes());
      String in_password = Base64.getEncoder().encodeToString(input_password.getBytes());
      boolean result = false;

      try {
         in_username = encryptDataByAES(in_username, aesUtil);
         in_password = encryptDataByAES(in_password, aesUtil);
         sendInfoInput(client, in_username, in_password);
         System.out.println("------Initial Information Transmition Done.------");
         // 接收server对login/register信息的反馈，信息都以base64存储
         String responseMsg = receiveInfoVerification(client, aesUtil);
         if( action.equals("login")){
            result = responseMsg.equals(Base64.getEncoder().encodeToString("Passed".getBytes()));
         }  else if( action.equals("register") ){
            statusCode = responseMsg;
            result = responseMsg.equals(Base64.getEncoder().encodeToString("250".getBytes()));
         }
      } catch (Exception e) {
         e.printStackTrace();
      } 
      return result;
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
      System.out.println("Received Server Public Key. ");
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
      System.out.println("Encrypted AES Key. ");
      return encData;
   }

   // 随机生成32位的nonce（4字节）
   private static int generateNonce() {
      SecureRandom randomFigure = new SecureRandom();
      byte[] keyBytes = new byte[4];
      randomFigure.nextBytes(keyBytes);
      int nonce = (keyBytes[0] & 0xff) | ((keyBytes[1] << 8) & 0xff00) | ((keyBytes[2] << 24) >>> 8) | (keyBytes[3] << 24);
      System.out.println("Generated Nonce. ");
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
      // System.out.println("The received data: " + verifyNonce);
      // 拿AES会话密钥解密
      verifyNonce = aesUtil.decryptAES(verifyNonce);
      // System.out.println("The received Base64 nonce: " + verifyNonce);
      // Base64逆编码
      verifyNonce = new String(Base64.getDecoder().decode(verifyNonce));
      // System.out.println("The server's nonce: "+verifyNonce);
      int verifyNonceInt = Integer.parseInt(verifyNonce);
      if (verifyNonceInt == nonce + 1)
         System.out.println("Receive from Server: the message:  " + verifyNonceInt + ". Verify passed.");
      else {
         System.out.println("Verify failed. Exit.");
         client.close();
         System.exit(1); // 终止程序
      }
   }

   private static void sendInfoInput(Socket client, String encUsr, String encPwd) throws IOException {
      OutputStream outToServer = client.getOutputStream();
      DataOutputStream out = new DataOutputStream(outToServer);
      out.writeUTF(encUsr);
      out.writeUTF(encPwd);
      System.out.println("Sent encrypted information to Server.");
   }

   private static String receiveInfoVerification(Socket client, cryptoAES aesUtil) throws IOException, Exception{
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
