package edu.campusloop.ui.dialog;

import edu.campusloop.core.service.AuthService;
import edu.campusloop.ui.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

public class RegisterDialog extends JDialog {

    private final AuthService authService;
    private final JTextField usernameField = new JTextField(16);
    private final JTextField nicknameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JPasswordField confirmField = new JPasswordField(16);

    public RegisterDialog(JFrame owner, AuthService authService) {
        super(owner, "注册新账号", true);
        this.authService = authService;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(createForm(), BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 0, 6, 10);

        addRow(panel, gbc, "用户名（3~20 位字母数字）", usernameField);
        addRow(panel, gbc, "昵称", nicknameField);
        addRow(panel, gbc, "密码（6~32 位，含字母和数字）", passwordField);
        addRow(panel, gbc, "确认密码", confirmField);

        JButton submit = new JButton("注册");
        JButton cancel = new JButton("取消");
        JPanel buttons = new JPanel();
        buttons.add(submit);
        buttons.add(cancel);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(14, 0, 0, 0);
        panel.add(buttons, gbc);

        submit.addActionListener(e -> doRegister());
        cancel.addActionListener(e -> dispose());
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String label, java.awt.Component field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private void doRegister() {
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();
        if (!Arrays.equals(password, confirm)) {
            UiUtils.showInfo(this, "两次输入的密码不一致");
            return;
        }
        try {
            authService.register(usernameField.getText(), new String(password), nicknameField.getText());
            UiUtils.showInfo(this, "注册成功，请使用新账号登录");
            dispose();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }
}
