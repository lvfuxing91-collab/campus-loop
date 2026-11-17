package edu.campusloop.ui.dialog;

import edu.campusloop.core.model.Item;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;

public class IntentDialog extends JDialog {

    private final UiContext context;
    private final Item item;
    private final JTextArea messageArea = new JTextArea(5, 30);

    public IntentDialog(Window owner, UiContext context, Item item) {
        super(owner, "发起流转意向", Dialog.ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.item = item;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        panel.add(new JLabel("物品：《" + item.getTitle() + "》"), BorderLayout.NORTH);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createTitledBorder("给发布者的留言（可选，最多 200 字）"));
        panel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        JButton submit = new JButton("提交意向");
        JButton cancel = new JButton("取消");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(cancel);
        actions.add(submit);
        panel.add(actions, BorderLayout.SOUTH);
        submit.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        return panel;
    }

    private void submit() {
        try {
            context.dealService().createIntent(item.getId(), context.currentUserId(), messageArea.getText());
            UiUtils.showInfo(this, "流转意向已提交");
            dispose();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }
}
