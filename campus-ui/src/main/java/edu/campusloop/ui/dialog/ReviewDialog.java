package edu.campusloop.ui.dialog;

import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Review;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;

/**
 * 提交评价对话框。
 * <p>
 * 成交后买卖双方各可评价一次；已有评价时展示历史，不再提供重复提交。
 */
public class ReviewDialog extends JDialog {

    private final UiContext context;
    private final Deal deal;
    private final JComboBox<Integer> ratingCombo = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
    private final JTextArea commentArea = new JTextArea(4, 24);
    private boolean submitted;

    public ReviewDialog(Window owner, UiContext context, Deal deal) {
        super(owner, "评价流转", Dialog.ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.deal = deal;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(new JLabel("物品：" + deal.getItemTitle()));
        info.add(Box.createVerticalStrut(4));
        info.add(new JLabel("对方：" + otherNickname()));
        info.add(Box.createVerticalStrut(4));
        info.add(new JLabel("状态：" + (deal.getStatus() == null ? "" : deal.getStatus().getDisplayName())));
        root.add(info, BorderLayout.NORTH);

        List<Review> existing = existingReviews();
        if (!existing.isEmpty()) {
            root.add(createExistingPanel(existing), BorderLayout.CENTER);
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton close = new JButton("关闭");
            close.addActionListener(e -> dispose());
            bottom.add(close);
            root.add(bottom, BorderLayout.SOUTH);
        } else {
            root.add(createForm(), BorderLayout.CENTER);
        }
        return root;
    }

    /** 当前用户对该笔流转已提交的评价。 */
    private List<Review> existingReviews() {
        try {
            return context.reviewService().reviewsOf(deal.getSellerId() == context.currentUserId()
                            ? deal.getBuyerId() : deal.getSellerId())
                    .stream()
                    .filter(r -> r.getDealId() == deal.getId()
                            && r.getReviewerId() == context.currentUserId())
                    .collect(java.util.stream.Collectors.toList());
        } catch (RuntimeException e) {
            return java.util.Collections.emptyList();
        }
    }

    private JPanel createExistingPanel(List<Review> existing) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("你已提交的评价"));
        for (Review review : existing) {
            String comment = review.getComment() == null || review.getComment().isEmpty()
                    ? "（无评论）" : review.getComment();
            panel.add(new JLabel(review.getRating() + " 星：" + comment));
            panel.add(Box.createVerticalStrut(4));
        }
        return panel;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("评分"), gbc);
        gbc.gridx = 1;
        form.add(ratingCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("评论"), gbc);
        gbc.gridx = 1;
        form.add(new JScrollPane(commentArea), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton submit = new JButton("提交评价");
        JButton cancel = new JButton("取消");
        buttons.add(submit);
        buttons.add(cancel);
        form.add(buttons, gbc);

        submit.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        return form;
    }

    private void submit() {
        try {
            int rating = (Integer) ratingCombo.getSelectedItem();
            context.reviewService().submit(deal.getId(), context.currentUserId(),
                    rating, commentArea.getText());
            submitted = true;
            UiUtils.showInfo(this, "评价已提交");
            dispose();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    public boolean isSubmitted() {
        return submitted;
    }

    private String otherNickname() {
        return context.currentUserId() == deal.getBuyerId()
                ? deal.getSellerNickname() : deal.getBuyerNickname();
    }
}
