package server;

import java.sql.Connection; // 导入用于创建数据库连接的类
import java.sql.DriverManager; // 导入用于管理数据库驱动的类
import java.sql.SQLException; // 导入用于处理SQL异常的类
import java.util.Base64; // 导入用于Base64编码和解码的类

public class DatabaseConnector {
    // 控制mysql的数据库名称
    private static final String URL = "jdbc:mysql://localhost:3306/your_database_name_please";
    // 登陆账号名，Base64编码
    private static final String USER = "your_username_please";
    // 账号的对应密码，Base64编码
    private static final String PASSWORD = "your_password_please";
    // 获得数据库名称
    private static final String dbName = "your_database_name_please";

    // 构造函数，加载MySQL驱动
    public DatabaseConnector() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 获取数据库连接，解码用户名和密码
    public Connection getConnection() throws SQLException {
        String usr = new String(Base64.getDecoder().decode(USER));
        String pwd = new String(Base64.getDecoder().decode(PASSWORD));
        return DriverManager.getConnection(URL, usr, pwd);
    }

    public static String getDatabaseName(){
        return dbName;
    }
}
