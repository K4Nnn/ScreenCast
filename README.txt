实现的功能：
1、密钥协商
    Server和Client希望共享一个会话密钥Ks。首先由服务器端生成RSA密钥对，并在连接之初将公钥送给客户端。
    随后客户端使用公钥加密Client生成的会话密钥送给Server，Server使用私钥解密。
    同时，Client、Server应当附上身份验证信息。eg. 随机数验证
    如何证明身份？发送信息包括RSA加密的AES会话密钥，以及AES会话密钥的Hash值（取SHA-256）。

Tip:1）在AES、RSA算法中。各密钥的存储都以Base64编码形式。即：传输，存储的过程中基本都是Base64编码。
    2）RSA类是类方法，而AES类是对象方法。

2、实现客户端登陆界面、服务器端存储数据于数据库。
    数据库采用MySQL；连接配置见server/DatabaseConnector.java

3、Server端启动后，监听来自Client的Socket通信连接（多线程）。Client成功登录后，启动屏幕内容捕获并投送至Server端。可以在client/ScreenCaptureSender.java下，修改传送帧率。

tip：编译命令：javac -cp "lib/mysql-connector-j-9.0.0.jar;src" -d bin -encoding UTF-8 src/server/*.java src/client/*.java src/cryptoUtils/*.java
    运行命令：
    启动server：java -cp "bin;lib/mysql-connector-j-9.0.0.jar" server.ServerTest 40101
    启动client：java -cp "bin;lib/mysql-connector-j-9.0.0.jar" client.ClientTest localhost 40101
注注：测试用。截至目前 2024/7/21 屏幕投送功能未完善。
