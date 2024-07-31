package javaGUI;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BeginFrame extends JFrame{
    private CardLayout cardLayout;
    // CardLayout顾名思义，像一张张卡片一样，每一次只显示最上面的那张卡片
    private JPanel mainPanel;
    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private int frame_width = 480;
    private int frame_height = 360;

    public static void main(String args[]){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BeginFrame beginFrame = new BeginFrame();
                beginFrame.setVisible(true);
            }
        });
    }

    public BeginFrame(){
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 添加Login和Register
        loginPanel = new LoginPanel();
        registerPanel = new RegisterPanel();
        mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");

        // 默认优先显示login
        cardLayout.show(mainPanel, "login");

        // 添加对切换功能的按钮监听
        loginPanel.setSwitchButtonListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "register");
                setTitle("ScreenCast Online - Register");
            }
        });

        registerPanel.setSwitchButtonListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "login");
                setTitle("ScreenCast Online - Login");
            }
        });

        add(mainPanel);
        setTitle("ScreenCast Online - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setSize(frame_width, frame_height);
        setVisible(true);
    }

    public LoginPanel getLoginPanel(){
        return this.loginPanel;
    }

    public RegisterPanel getRegisterPanel(){
        return this.registerPanel;
    }

}
