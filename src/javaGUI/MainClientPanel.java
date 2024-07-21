package javaGUI;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MainClientPanel extends JPanel{

    private static JLabel noteLabel;
    private static JButton terminateButton;

    public MainClientPanel(){
        // 使用BorderLayout布局
        setLayout(new BorderLayout());

        // 创建辅助面板，用于放置label和button
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // 创建标签提示
        noteLabel = new JLabel();
        String noteText = "Casting......";
        noteLabel.setText(noteText);

        // 创建程序终止按钮
        terminateButton = new JButton("Terminate");

        // 辅助面板添加label和button
        centerPanel.add(noteLabel);
        centerPanel.add(terminateButton);

        // 创建一个容器面板，使用GridBagLayout布局来居中对齐centerPanel
        JPanel containerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        containerPanel.add(centerPanel, gbc);

        // 将辅助面板放置在BorderLayout的中间位置
        add(containerPanel, BorderLayout.CENTER);
    }

    public void setTerminateListener(ActionListener actionListener){
        terminateButton.addActionListener(actionListener);
    }

    public void setNoteLabelText(String text){
        noteLabel.setText(text);
    }
}
