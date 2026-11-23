package edu.campusloop.ui.panel;

import edu.campusloop.core.model.Item;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;
import edu.campusloop.ui.dialog.ItemDetailDialog;
import edu.campusloop.ui.table.ItemTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;

public class FavoritesPanel extends JPanel {

    private final UiContext context;
    private final ItemTableModel model = new ItemTableModel();
    private final JTable table = new JTable(model);

    public FavoritesPanel(UiContext context) {
        super(new BorderLayout(8, 8));
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton detailButton = new JButton("查看详情");
        JButton removeButton = new JButton("取消收藏");
        JButton refreshButton = new JButton("刷新");
        toolbar.add(detailButton);
        toolbar.add(removeButton);
        toolbar.add(refreshButton);
        JLabel tip = new JLabel("收藏的物品下架后仍会保留在这里");
        tip.setForeground(Color.GRAY);
        toolbar.add(tip);
        add(toolbar, BorderLayout.NORTH);

        table.setRowHeight(26);
        add(new JScrollPane(table), BorderLayout.CENTER);

        detailButton.addActionListener(e -> openDetail());
        removeButton.addActionListener(e -> removeFavorite());
        refreshButton.addActionListener(e -> refresh());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    openDetail();
                }
            }
        });

        refresh();
    }

    public void refresh() {
        try {
            model.setItems(context.itemService().myFavorites(context.currentUserId()));
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void removeFavorite() {
        Item item = selectedItem("请先选择要取消收藏的物品");
        if (item == null) {
            return;
        }
        try {
            context.itemService().unfavorite(context.currentUserId(), item.getId());
            refresh();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void openDetail() {
        Item item = selectedItem("请先选择要查看的物品");
        if (item == null) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        new ItemDetailDialog(owner, context, item).setVisible(true);
        refresh();
    }

    private Item selectedItem(String message) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UiUtils.showInfo(this, message);
            return null;
        }
        return model.getItemAt(table.convertRowIndexToModel(viewRow));
    }
}
