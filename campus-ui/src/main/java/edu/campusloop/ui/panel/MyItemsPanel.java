package edu.campusloop.ui.panel;

import edu.campusloop.core.model.Item;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;
import edu.campusloop.ui.dialog.PublishItemDialog;
import edu.campusloop.ui.table.ItemTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

public class MyItemsPanel extends JPanel {

    private final UiContext context;
    private final ItemTableModel model = new ItemTableModel();
    private final JTable table = new JTable(model);

    public MyItemsPanel(UiContext context) {
        super(new BorderLayout(8, 8));
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton publishButton = new JButton("发布闲置物品");
        JButton editButton = new JButton("编辑");
        JButton offShelfButton = new JButton("下架");
        JButton refreshButton = new JButton("刷新");
        toolbar.add(publishButton);
        toolbar.add(editButton);
        toolbar.add(offShelfButton);
        toolbar.add(refreshButton);
        add(toolbar, BorderLayout.NORTH);

        table.setRowHeight(26);
        add(new JScrollPane(table), BorderLayout.CENTER);

        publishButton.addActionListener(e -> openDialog(null));
        editButton.addActionListener(e -> {
            Item item = selectedItem();
            if (item != null) {
                openDialog(item);
            }
        });
        offShelfButton.addActionListener(e -> offShelf());
        refreshButton.addActionListener(e -> refresh());

        refresh();
    }

    public void refresh() {
        try {
            model.setItems(context.itemService().myItems(context.currentUserId()));
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void openDialog(Item item) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new PublishItemDialog(owner, context, item).setVisible(true);
        refresh();
    }

    private void offShelf() {
        Item item = selectedItem();
        if (item == null) {
            return;
        }
        if (!UiUtils.confirm(this, "确定下架《" + item.getTitle() + "》吗？")) {
            return;
        }
        try {
            context.itemService().offShelf(context.currentUserId(), item.getId());
            refresh();
            UiUtils.showInfo(this, "已下架");
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private Item selectedItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UiUtils.showInfo(this, "请先在列表中选择一个物品");
            return null;
        }
        return model.getItemAt(table.convertRowIndexToModel(viewRow));
    }
}
