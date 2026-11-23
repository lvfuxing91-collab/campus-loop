package edu.campusloop.ui.dialog;

import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.WindowConstants;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/** 修改当前账号密码。 */
public class ChangePasswordDialog extends JDialog {

    private final UiContext context;
    private final JPasswordField oldPasswordField = new JPasswordField(18);
    private final JPasswordField newPasswordField = new JPasswordField(18);
    private final JPasswordField confirmPasswordField = new JPasswordField(18);

    public ChangePasswordDialog(Window owner, UiContext context) {
        super(owner, "修改密码", Dialog.ModalityType.APPLICATION_MODAL);
        this.context = context;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 14, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        addRow(panel, gbc, 0, "原密码", oldPasswordField);
        addRow(panel, gbc, 1, "新密码", newPasswordField);
        addRow(panel, gbc, 2, "确认新密码", confirmPasswordField);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton submit = new JButton("确认修改");
        JButton cancel = new JButton("取消");
        actions.add(submit);
        actions.add(cancel);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(actions, gbc);

        submit.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(submit);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label,
                        JPasswordField field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void submit() {
        String oldPassword = new String(oldPasswordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirmation = new String(confirmPasswordField.getPassword());
        if (!newPassword.equals(confirmation)) {
            UiUtils.showInfo(this, "两次输入的新密码不一致");
            return;
        }
        try {
            context.authService().changePassword(context.currentUserId(), oldPassword, newPassword);
            UiUtils.showInfo(this, "密码修改成功，下次登录请使用新密码");
            dispose();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }
}
