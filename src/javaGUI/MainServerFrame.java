package javaGUI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class MainServerFrame extends JFrame {
    private JPanel toolPanel;
    private JPanel displayPanel;
    private JPanel mainPanel;
    private TitledBorder toolBorder;
    private TitledBorder displayBorder;
    private JLabel toolLabel;
    private JTextField freqField;
    private String freqString = "24";
    private JButton freqButton;
    private BorderLayout borderLayout;
    private FlowLayout toolLayout;
    private FlowLayout displayLayout;
    private boolean isToolButtonClicked = false;
    private int frame_width = 600;
    private int frame_height = 450;

    public MainServerFrame() {
        mainPanel = new JPanel();
        toolPanel = new JPanel();
        displayPanel = new JPanel();
        toolBorder = BorderFactory.createTitledBorder("工具栏");
        displayBorder = BorderFactory.createTitledBorder("显示栏");
        toolLabel = new JLabel("设置帧率");
        freqField = new JTextField(freqString, 6);
        freqButton = new JButton("设置");
        borderLayout = new BorderLayout();

        // 接收键盘输入时，只接受数字
		freqField.addKeyListener(new KeyAdapter(){
			public void keyTyped(KeyEvent e) {
				int keyChar = e.getKeyChar();				
				if(keyChar >= KeyEvent.VK_0 && keyChar <= KeyEvent.VK_9){
                    // none
				}else{
					e.consume(); //关键，屏蔽掉非法输入
				}
			}
		});

        // 监听Button
        freqButton.addActionListener(new ButtonClickListener());

        // 对于toolPanel，采用流布局，居中
        toolLayout = new FlowLayout(FlowLayout.LEADING);
        toolPanel.setPreferredSize(new Dimension(frame_width, 80)); // 设置工具面板的首选尺寸
        toolPanel.setLayout(toolLayout);
        toolPanel.setBorder(toolBorder);
        toolPanel.add(toolLabel);
        toolPanel.add(freqField);
        toolPanel.add(freqButton);

        // 对于displayPanel，采用流布局
        displayLayout = new FlowLayout(FlowLayout.LEADING, 20, 10);
        displayPanel.setLayout(displayLayout);
        displayPanel.setBorder(displayBorder);

        // 添加工具栏、展示栏，并设置竖直布局
        mainPanel.setLayout(borderLayout);
        mainPanel.add(toolPanel, BorderLayout.NORTH);
        mainPanel.add(displayPanel, BorderLayout.CENTER);

        // Frame放入Panel
        add(mainPanel);

        // 设置JFrame初始化
        setTitle("ScreenCast Online - Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setSize(frame_width, frame_height);
        setVisible(true);
    }

    public void addDisplayComp(Component comp) {
        displayPanel.add(comp);
        displayPanel.revalidate(); // 添加组件后重新布局
        displayPanel.repaint(); // 重绘组件
    }

    public String getFrequency(){
        return this.freqString;
    }

    public void setToolButtonClicked(boolean no){
        this.isToolButtonClicked = no;
    }

    public boolean getToolButtonClicked(){
        return isToolButtonClicked;
    }

    public void setButtonActionListener(ActionListener al){
        freqButton.addActionListener(al);
    }

    private class ButtonClickListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            String content = freqField.getText();
            freqString = content;
            isToolButtonClicked = true;
        }
    }
}
