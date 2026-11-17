package edu.campusloop.ui.dialog;

import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.util.DateTimes;
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
import java.awt.GridLayout;
import java.awt.Window;

public class ItemDetailDialog extends JDialog {

    private final UiContext context;
    private Item item;

    public ItemDetailDialog(Window owner, UiContext context, Item selected) {
        super(owner, "物品详情", Dialog.ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.item = selected;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        loadDetail();
        setContentPane(createContent());
        setSize(560, 430);
        setLocationRelativeTo(owner);
    }

    private void loadDetail() {
        try {
            item = context.itemService().detail(item.getId());
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));
        JLabel title = new JLabel(item.getTitle());
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        panel.add(title, BorderLayout.NORTH);

        JPanel facts = new JPanel(new GridLayout(0, 2, 8, 8));
        facts.add(new JLabel("分类：" + item.getCategoryName()));
        facts.add(new JLabel("发布者：" + item.getOwnerNickname()));
        facts.add(new JLabel("交易方式：" + item.getTradeType().getDisplayName()));
        facts.add(new JLabel("成色：" + item.getCondition().getDisplayName()));
        facts.add(new JLabel("价格：" + item.getPrice().stripTrailingZeros().toPlainString() + " 元"));
        facts.add(new JLabel("状态：" + item.getStatus().getDisplayName()));
        facts.add(new JLabel("浏览量：" + item.getViewCount()));
        facts.add(new JLabel("发布时间：" + DateTimes.format(item.getCreatedAt())));

        JTextArea description = new JTextArea(item.getDescription());
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(BorderFactory.createTitledBorder("物品描述"));
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.add(facts, BorderLayout.NORTH);
        center.add(new JScrollPane(description), BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        JButton intent = new JButton("发起流转意向");
        JButton close = new JButton("关闭");
        boolean canIntent = item.getOwnerId() != context.currentUserId()
                && item.getStatus() == ItemStatus.ON_SALE;
        intent.setEnabled(canIntent);
        intent.setToolTipText(canIntent ? null : "仅可对他人的在售物品发起意向");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(intent);
        actions.add(close);
        panel.add(actions, BorderLayout.SOUTH);
        intent.addActionListener(e -> new IntentDialog(this, context, item).setVisible(true));
        close.addActionListener(e -> dispose());
        return panel;
    }
}
