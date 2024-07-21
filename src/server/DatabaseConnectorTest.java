package server;

import java.sql.Connection; // 导入用于创建数据库连接的类
import java.sql.ResultSet; // 导入用于处理结果集的类
import java.sql.SQLException; // 导入用于处理SQL异常的类
import java.sql.Statement; // 导入用于执行SQL语句的类

public class DatabaseConnectorTest {
    public static void main(String[] args) {
        DatabaseConnector databaseConnector = new DatabaseConnector(); // 创建DatabaseConnector对象
        try (Connection connection = databaseConnector.getConnection()) { // 获取数据库连接
            if (connection != null) {
                System.out.println("Connected to the database!"); // 连接成功

                // SQL查询语句
                String query = "SELECT * FROM users";
                try (Statement statement = connection.createStatement(); // 创建Statement对象
                     ResultSet resultSet = statement.executeQuery(query)) { // 执行查询并获取结果集

                    // 遍历结果集并输出
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id"); // 获取id字段
                        String username = resultSet.getString("username"); // 获取username字段
                        String password = resultSet.getString("password"); // 获取password字段

                        // 输出用户信息
                        System.out.println("ID: " + id + ", Username: " + username + ", Password: " + password);
                    }
                } catch (SQLException e) {
                    e.printStackTrace(); // 处理SQL异常
                }
            }
        } catch (SQLException e) {
            System.out.println("Connection failed!"); // 连接失败
            e.printStackTrace(); // 处理连接异常
        }
    }
}
