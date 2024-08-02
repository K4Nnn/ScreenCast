package server;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPair;
import java.sql.*;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.*;

import cryptoUtils.*;
import javaGUI.MainServerFrame;

public class ServerTest {
    private ServerSocket serverSocket;
    private Map<String, String> sessionKeyMap;
    private DatabaseConnector dbConnector;
    private MainServerFrame serverFrame;

    public ServerTest(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(60000);
        sessionKeyMap = new ConcurrentHashMap<>();
        dbConnector = new DatabaseConnector(); // 初始化数据库连接
        serverFrame = new MainServerFrame();
    }

    public void start() {
        while (true) {
            try {
                System.out.println("Waiting for connection on port: " + serverSocket.getLocalPort()+
                        ", "+Thread.activeCount()+" threads now.");
                serverFrame.setVisible(true);
                Socket server = serverSocket.accept();
                System.out.println("Connection received from: " + server.getRemoteSocketAddress());
                new ClientHandler(server, sessionKeyMap, dbConnector).start();
            } catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try {
            ServerTest server = new ServerTest(port);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ClientHandler extends Thread {
        private Socket server;
        private Map<String, String> sessionKeyMap;
        private DatabaseConnector dbConnector;
        private cryptoAES aesUtil;
        private String rsaPublicKey;
        private String rsaPrivateKey;
    
        public ClientHandler(Socket server, Map<String, String> sessionKeyMap, DatabaseConnector dbConnector) {
            this.server = server;
            this.sessionKeyMap = sessionKeyMap;
            this.dbConnector = dbConnector;
            this.aesUtil = new cryptoAES();
            try {
                // 生成RSA密钥对并获取公钥和私钥
                KeyPair keyPair = generateRSAKeys();
                rsaPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
                rsaPrivateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        public void run() {
            try {
                sendRSAPublicKeyToClient();
                String encryptedData = receiveEncryptedDataFromClient();
                int nonce = processEncryptedData(encryptedData);
                sendNonceToClient(nonce);
                String preMsg = receivePreMessage();
                if (preMsg.equals("1")){
                    if( processLoginMsgFromClient() ){ //处理登录请求（if
                        addUserScreen();
                    }
                }
                else if (preMsg.equals("2")){
                    System.out.println("Ready to receive register data.");
                    processRegisterMsgFromClient();
                }
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        private void addUserScreen() {
            try {
                JPanel displayPanel = new JPanel();
                displayPanel.setBorder(BorderFactory.createEtchedBorder());
                displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
                JLabel ipLabel = new JLabel();
                InetAddress ip = server.getInetAddress();
                ipLabel.setText("Name: "+ip.getHostName()+"("+ip.getHostAddress()+")");
                JLabel imageLabel = new JLabel();
                int labelWidth = 300;
                int labelHeight = 225;
                InputStream is = server.getInputStream();
                DataInputStream dis = new DataInputStream(is);
                displayPanel.add(ipLabel);
                displayPanel.add(imageLabel);
                serverFrame.addDisplayComp(displayPanel);
                while ( !server.isClosed() ) {
                    int size = dis.readInt(); // 读取图像大小
                    byte[] imageBytes = new byte[size];
                    dis.readFully(imageBytes); // 确保读取完整的图像数据
        
                    ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                    BufferedImage image = ImageIO.read(bais);
                    if (image != null) {
                        // 确保图片的缩放比例不变
                        int imgWidth = image.getWidth();
                        int imgHeight = image.getHeight();
                        double imgAspect = (double)imgWidth / imgHeight;
                        double labelAspect = (double)labelWidth / labelHeight;
                        int showWidth = labelWidth;
                        int showHeight = labelHeight;
                        if( imgAspect < labelAspect )
                            showWidth = (int)(showHeight * imgAspect);
                        else if ( imgAspect > labelAspect)
                            showHeight = (int)(showWidth / imgAspect);
                        // 以正确de比例创建image
                        Image scaledImg = image.getScaledInstance(showWidth, showHeight, Image.SCALE_SMOOTH);
                        ImageIcon icon = new ImageIcon(scaledImg);
                        imageLabel.setIcon(icon);
                        serverFrame.validate();
                        serverFrame.repaint();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }    
    
        // 生成RSA密钥对的方法
        private KeyPair generateRSAKeys() throws Exception {
            cryptoRSA.generateKeyToFile(cryptoRSA.ALGORITHM, "src/server/rsa_public_key.txt", "src/server/rsa_private_key.txt");
            PublicKey pubKey = cryptoRSA.loadPublicKeyFromFile(cryptoRSA.ALGORITHM, "src/server/rsa_public_key.txt");
            PrivateKey priKey = cryptoRSA.loadPrivateKeyFromFile(cryptoRSA.ALGORITHM, "src/server/rsa_private_key.txt");
            return new KeyPair(pubKey, priKey);
        }
    
        private void sendRSAPublicKeyToClient() throws IOException {
            System.out.println("---Transmitting RSA public key to Client......");
            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            if ( rsaPublicKey != null ){
                out.writeUTF(rsaPublicKey);
                System.out.println("---Transmission over.");
            }
            else
                System.out.println("The Server's RSA pub-key is null!");
        }
    
        private String receiveEncryptedDataFromClient() throws IOException {
            DataInputStream in = new DataInputStream(server.getInputStream());
            return in.readUTF();
        }
    
        // 缺少步骤：nonce的加密解密
        // 解出nonce后，将其+1再加密传输
        private int processEncryptedData(String getInput) throws Exception {
            String sessionKey = extractSessionKey(getInput);
            String nonceStr = extractNonce(getInput);
            int nonceInt = Integer.parseInt(nonceStr);
    
            // 获取随机向量iv
            InputStream inFromClient = server.getInputStream();
            byte[] AESiv = inFromClient.readNBytes(16); //读取16字节长的iv
            aesUtil.setIV(AESiv);
    
            sessionKey = cryptoRSA.decryptRSA(cryptoRSA.ALGORITHM, sessionKey, 
                    cryptoRSA.loadPrivateKeyFromString(cryptoRSA.ALGORITHM, rsaPrivateKey), cryptoRSA.MAX_DECRYPT_SIZE);
            sessionKeyMap.put(server.getRemoteSocketAddress().toString(), sessionKey);
            aesUtil.setKey(sessionKey);
            
            return nonceInt+1;
        }
    
        private void sendNonceToClient(int nonce) throws IOException, Exception{
            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            // 获取nonce变为String后的Base64编码
            String nonceBase64 = Base64.getEncoder().encodeToString(String.valueOf(nonce).getBytes());
            // 将Base64编码加密后传输
            String nonceStr = aesUtil.encryptAES(nonceBase64);
            out.writeUTF(nonceStr);
            System.out.println("Nonce successfully sent. ");
        }
    
        private String receivePreMessage() throws IOException, Exception{
            DataInputStream in = new DataInputStream(server.getInputStream());
            String preMsg = aesUtil.decryptAES(in.readUTF());
            System.out.println("Server received premessage: "+preMsg);
            return preMsg;
        }
    
        private boolean processLoginMsgFromClient() throws IOException{
            // 接收Client的输入
            DataInputStream in = new DataInputStream(server.getInputStream());
            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            String encUsr = in.readUTF();
            String encPwd = in.readUTF();
            String usr = null;
            String pwd = null;
            boolean isLogin = false;
            try{
                usr = decryptDataByAES(encUsr, aesUtil);
                pwd = decryptDataByAES(encPwd, aesUtil);
            }catch(Exception e){
                e.printStackTrace();
            }
            // 默认不以明文存储，usr和pwd都是Base64编码形式
            // 对接受的login信息在数据库中验证
            DatabaseConnector databaseConnector = new DatabaseConnector(); // 创建DatabaseConnector对象
            try (Connection connection = databaseConnector.getConnection()) { // 获取数据库连接
                if (connection != null) {
                    // 连接成功
                    System.out.println("Connected to the database: "+DatabaseConnector.getDatabaseName()+"!"); 
    
                    // SQL查询语句
                    String query = "SELECT * FROM users";
                    try(Statement statement = connection.createStatement(); // 创建Statement对象
                        ResultSet resultSet = statement.executeQuery(query)) { // 执行查询并获取结果集
                    
                        // 遍历结果集并输出
                        while (resultSet.next()) {
                            String username = resultSet.getString("username"); // 获取username字段
                            String password = resultSet.getString("password"); // 获取password字段
                            if( Base64.getEncoder().encodeToString(username.getBytes()).equals(usr) && 
                                    Base64.getEncoder().encodeToString(password.getBytes()).equals(pwd) ){
                                isLogin = true;
                                String str_varify = Base64.getEncoder().encodeToString("Passed".getBytes());
                                out.writeUTF(aesUtil.encryptAES(str_varify));;
                                // 输出用户信息
                                System.out.println("Login correct for user: "+username);
                                // 下面有待添加登录成功的返回信息
                                break;
                            }
                        }
    
                        if ( !isLogin ) {
                            String str_varify = Base64.getEncoder().encodeToString("Failed".getBytes());
                            out.writeUTF(aesUtil.encryptAES(str_varify));
                            System.out.println("Login failed!");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace(); // 处理SQL异常
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                System.out.println("Connection failed!"); // 连接失败
                e.printStackTrace(); // 处理连接异常
            }
            return isLogin;
        }
    
        private boolean processRegisterMsgFromClient() throws IOException{
            // 接收Client的输入
            DataInputStream in = new DataInputStream(server.getInputStream());
            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            System.out.println("Begin receiving usr and pwd.");
            String encUsr = in.readUTF();
            String encPwd = in.readUTF();
            String usr = null;
            String pwd = null;
            boolean isRegistered = false;
            
            try{
                usr = new String(Base64.getDecoder().decode(decryptDataByAES(encUsr, aesUtil)));
                pwd = new String(Base64.getDecoder().decode(decryptDataByAES(encPwd, aesUtil)));
            }catch(Exception e){
                e.printStackTrace();
            }
            System.out.println("Successfully get usr and pwd.");
            // 获取客户端IP地址
            String clientIpAddress = server.getInetAddress().getHostAddress();
            System.out.println("client's ipAd: "+clientIpAddress);
            
            // 默认不以明文存储，usr和pwd都是Base64编码形式
            // 对接收的数据信息在数据库中验证是否重复
            DatabaseConnector databaseConnector = new DatabaseConnector(); // 创建DatabaseConnector对象
            try (Connection connection = databaseConnector.getConnection()) { // 获取数据库连接
                if (connection != null) {
                    // 连接成功
                    System.out.println("Connected to the database: "+DatabaseConnector.getDatabaseName()+"!"); 
        
                    // 检查用户名是否已经存在
                    String checkUsernameQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
                    try (PreparedStatement checkUsernameStmt = connection.prepareStatement(checkUsernameQuery)) {
                        checkUsernameStmt.setString(1, Base64.getEncoder().encodeToString(usr.getBytes()));
                        try (ResultSet rs = checkUsernameStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                // 用户名已存在
                                System.out.println("Registration failed: Username exists!");
                                String str_varify = Base64.getEncoder().encodeToString("401".getBytes());
                                out.writeUTF(aesUtil.encryptAES(str_varify));
                                return false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
        
                    // 检查IP地址注册数量
                    String checkIpQuery = "SELECT COUNT(*) FROM users WHERE id = ?";
                    try (PreparedStatement checkIpStmt = connection.prepareStatement(checkIpQuery)) {
                        checkIpStmt.setString(1, clientIpAddress);
                        try (ResultSet rs = checkIpStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) >= 5) {
                                // IP地址注册次数超过限制
                                System.out.println("Registration failed: IP limit exceeded!");
                                String str_varify = Base64.getEncoder().encodeToString("403".getBytes());
                                out.writeUTF(aesUtil.encryptAES(str_varify));
                                return false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
        
                    // 插入新的用户数据
                    String insertQuery = "INSERT INTO users (id, username, password) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, clientIpAddress);
                        insertStmt.setString(2, usr);
                        insertStmt.setString(3, pwd);
                        insertStmt.executeUpdate();
                        isRegistered = true;
                        System.out.println("Registration successful for user: " + usr);
                        String str_varify = Base64.getEncoder().encodeToString("250".getBytes());
                        out.writeUTF(aesUtil.encryptAES(str_varify));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                System.out.println("Connection failed!"); // 连接失败
                e.printStackTrace(); // 处理连接异常
            }
            return isRegistered;
        }
        
    
        private String decryptDataByAES(String encData, cryptoAES aesUtil) throws Exception{
            return aesUtil.decryptAES(encData);
        }
    
        private String extractSessionKey(String data) {
            return data.split("\n")[0];
        }
    
        private String extractNonce(String data) {
            return data.split("\n")[1];
        }
    }
}


