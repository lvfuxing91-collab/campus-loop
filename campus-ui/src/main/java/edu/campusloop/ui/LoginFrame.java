package edu.campusloop.ui;

import edu.campusloop.core.model.User;
import edu.campusloop.ui.dialog.RegisterDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginFrame extends JFrame {

    private final UiContext context;
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton loginButton = new JButton("登  录");
    private final JButton registerButton = new JButton("注册新账号");

    public LoginFrame(UiContext context) {
        super("低碳校园资源循环流转系统 · 登录");
        this.context = context;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(createContent());
        pack();
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(createBrandPanel(), BorderLayout.WEST);
        root.add(createFormPanel(), BorderLayout.CENTER);
        return root;
    }

    private JPanel createBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0x1F, 0x7A, 0x4D));
        panel.setPreferredSize(new Dimension(300, 380));

        Box box = Box.createVerticalBox();
        JLabel title = new JLabel("低碳校园");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("资源循环流转系统");
        subtitle.setFont(new Font("Microsoft YaHei", Font.PLAIN, 18));
        subtitle.setForeground(new Color(0xDD, 0xF2, 0xE4));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        JLabel slogan = new JLabel("让闲置物品流动起来");
        slogan.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        slogan.setForeground(new Color(0xC8, 0xE6, 0xD5));
        slogan.setAlignmentX(LEFT_ALIGNMENT);

        box.add(title);
        box.add(Box.createVerticalStrut(8));
        box.add(subtitle);
        box.add(Box.createVerticalStrut(28));
        box.add(slogan);
        panel.add(box);
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 20, 0);

        JLabel heading = new JLabel("账号登录");
        heading.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        panel.add(heading, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 0, 6, 10);
        gbc.gridy++;
        panel.add(new JLabel("用户名"), gbc);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("密  码"), gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(22, 0, 8, 0);
        JPanel buttons = new JPanel();
        buttons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        buttons.add(loginButton);
        buttons.add(registerButton);
        panel.add(buttons, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 0, 0);
        JLabel hint = new JLabel("默认管理员：admin / Admin@123");
        hint.setForeground(Color.GRAY);
        panel.add(hint, gbc);

        loginButton.addActionListener(e -> doLogin());
        registerButton.addActionListener(e -> new RegisterDialog(this, context.authService()).setVisible(true));
        return panel;
    }

    private void doLogin() {
        try {
            User user = context.authService().login(usernameField.getText(), new String(passwordField.getPassword()));
            context.setCurrentUser(user);
            dispose();
            new MainFrame(context).setVisible(true);
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }
}
