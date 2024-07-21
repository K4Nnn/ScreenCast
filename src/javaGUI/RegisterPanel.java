package javaGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RegisterPanel extends JPanel {
    
    private String usernameText;
    private String passwordText;
    private JButton registerButton;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel themeLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private boolean isRegisterClicked;

    public RegisterPanel(){
        // 创建面板，设置边框布局
        setLayout(new BorderLayout());

        // 创建标题面板
        JPanel themePanel = new JPanel();
        themeLabel = new JLabel("ScreenCast Online"); 
        themeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        themeLabel.setFont(new Font("仿宋", Font.BOLD, 25));
        themePanel.add(themeLabel);
        add(themePanel, BorderLayout.NORTH);

        // 创建放置于CENTER的面板
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // 创建文本域、密码域对象
        usernameField = new JTextField(32);
        passwordField = new JPasswordField(32);
        // 创建文本、密码对应标签
        usernameLabel = new JLabel("Username", JLabel.CENTER);
        passwordLabel = new JLabel("Password", JLabel.CENTER);

        // 1. 创建username信息的panel
        JPanel usernamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        usernamePanel.add(usernameLabel);
        usernamePanel.add(usernameField);

        // 2. 创建password信息的panel
        JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        passwordPanel.add(passwordLabel);
        passwordPanel.add(passwordField);

        // 将username和password面板添加至中心面板
        centerPanel.add(usernamePanel);
        centerPanel.add(passwordPanel);
        add(centerPanel, BorderLayout.CENTER);

        // 创建button，并设置监听事件
        registerButton = new JButton("Register");
        registerButton.addActionListener(new RegisterAction());

        // button放置于BorderLayou.SOUTH
        add(registerButton, BorderLayout.SOUTH);
    }

    public String getUsername() {
        return usernameText;
    }

    public String getPassword() {
        return passwordText;
    }
    
    public void setRegisterClicked(boolean no){
        this.isRegisterClicked = no;
    }

    public boolean getRegisterClicked(){
        return this.isRegisterClicked;
    }

    private class RegisterAction implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            usernameText = usernameField.getText();
            passwordText = new String(passwordField.getPassword());
            isRegisterClicked = true;
        }
    }
}
