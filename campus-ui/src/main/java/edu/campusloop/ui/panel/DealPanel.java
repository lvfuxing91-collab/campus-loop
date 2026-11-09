package edu.campusloop.ui.panel;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.model.Deal;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;
import edu.campusloop.ui.dialog.ReviewDialog;
import edu.campusloop.ui.table.DealTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * 流转记录页。
 * <p>
 * 展示当前用户作为买家或卖家参与的全部流转，并按当前状态提供
 * 接受、拒绝、完成、取消与评价操作。可执行的按钮随选中记录的状态变化。
 */
public class DealPanel extends JPanel {

    private final UiContext context;
    private final DealTableModel model = new DealTableModel();
    private final JTable table = new JTable(model);
    private final JButton acceptButton = new JButton("接受");
    private final JButton rejectButton = new JButton("拒绝");
    private final JButton completeButton = new JButton("确认完成");
    private final JButton cancelButton = new JButton("取消意向");
    private final JButton reviewButton = new JButton("评价");
    private final JButton refreshButton = new JButton("刷新");

    public DealPanel(UiContext context) {
        super(new BorderLayout(8, 8));
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(acceptButton);
        toolbar.add(rejectButton);
        toolbar.add(completeButton);
        toolbar.add(cancelButton);
        toolbar.add(reviewButton);
        toolbar.add(refreshButton);
        add(toolbar, BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel tip = new JLabel("作为买家可发起意向、取消或评价；作为卖家可接受、拒绝或确认完成");
        tip.setForeground(Color.GRAY);
        add(tip, BorderLayout.SOUTH);

        acceptButton.addActionListener(e -> accept());
        rejectButton.addActionListener(e -> reject());
        completeButton.addActionListener(e -> complete());
        cancelButton.addActionListener(e -> cancel());
        reviewButton.addActionListener(e -> review());
        refreshButton.addActionListener(e -> refresh());
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());

        refresh();
    }

    public void refresh() {
        try {
            // 记住当前选中的流转ID，重建数据后尽量恢复选中，避免操作后按钮状态失效
            Deal selected = selectedDeal(false);
            long keepId = selected == null ? -1L : selected.getId();

            model.setDeals(context.dealService().myDeals(context.currentUserId()),
                    context.currentUserId());

            if (keepId > 0) {
                for (int row = 0; row < model.getRowCount(); row++) {
                    if (model.getDealAt(row).getId() == keepId) {
                        table.setRowSelectionInterval(row, row);
                        break;
                    }
                }
            }
            updateButtons();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void updateButtons() {
        Deal deal = selectedDeal(false);
        boolean pending = deal != null && deal.getStatus() == DealStatus.PENDING;
        boolean accepted = deal != null && deal.getStatus() == DealStatus.ACCEPTED;
        boolean completed = deal != null && deal.getStatus() == DealStatus.COMPLETED;
        boolean seller = deal != null && deal.getSellerId() == context.currentUserId();

        acceptButton.setEnabled(pending && seller);
        rejectButton.setEnabled(pending && seller);
        completeButton.setEnabled(accepted && seller);
        cancelButton.setEnabled(pending || accepted);
        reviewButton.setEnabled(completed);
    }

    private void accept() {
        Deal deal = requireSelected();
        if (deal == null || !UiUtils.confirm(this, "接受对《" + deal.getItemTitle() + "》的流转意向？")) {
            return;
        }
        run(() -> context.dealService().accept(deal.getId(), context.currentUserId()), "已接受意向");
    }

    private void reject() {
        Deal deal = requireSelected();
        if (deal == null) {
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "拒绝原因（可留空）：", "拒绝意向",
                JOptionPane.PLAIN_MESSAGE);
        if (reason == null) {
            return;
        }
        run(() -> context.dealService().reject(deal.getId(), context.currentUserId(), reason), "已拒绝意向");
    }

    private void complete() {
        Deal deal = requireSelected();
        if (deal == null || !UiUtils.confirm(this, "确认《" + deal.getItemTitle() + "》已完成流转？"
                + "\n确认后双方可以互相评价。")) {
            return;
        }
        run(() -> context.dealService().complete(deal.getId(), context.currentUserId()), "流转已完成");
    }

    private void cancel() {
        Deal deal = requireSelected();
        if (deal == null || !UiUtils.confirm(this, "取消这条流转意向？")) {
            return;
        }
        run(() -> context.dealService().cancel(deal.getId(), context.currentUserId()), "已取消意向");
    }

    private void review() {
        Deal deal = requireSelected();
        if (deal == null) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        ReviewDialog dialog = new ReviewDialog(owner, context, deal);
        dialog.setVisible(true);
        if (dialog.isSubmitted()) {
            refresh();
        }
    }

    private void run(Runnable action, String successMessage) {
        try {
            action.run();
            refresh();
            UiUtils.showInfo(this, successMessage);
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private Deal requireSelected() {
        Deal deal = selectedDeal(true);
        return deal;
    }

    /** 取当前选中记录；未选中时按提示参数决定是否弹窗。 */
    private Deal selectedDeal(boolean notify) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            if (notify) {
                UiUtils.showInfo(this, "请先在列表中选择一条流转记录");
            }
            return null;
        }
        return model.getDealAt(table.convertRowIndexToModel(viewRow));
    }
}
