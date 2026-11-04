package edu.campusloop.ui.panel;

import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel(String title, String message) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new java.awt.Font("Microsoft YaHei", java.awt.Font.BOLD, 20));
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setForeground(Color.GRAY);
        messageLabel.setAlignmentX(CENTER_ALIGNMENT);

        content.add(javax.swing.Box.createVerticalGlue());
        content.add(titleLabel);
        content.add(javax.swing.Box.createVerticalStrut(10));
        content.add(messageLabel);
        content.add(javax.swing.Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
    }
}
