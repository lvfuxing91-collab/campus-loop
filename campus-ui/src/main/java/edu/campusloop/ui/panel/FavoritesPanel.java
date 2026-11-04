package edu.campusloop.ui.panel;

import edu.campusloop.core.model.Item;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;
import edu.campusloop.ui.table.ItemTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

public class FavoritesPanel extends JPanel {

    private final UiContext context;
    private final ItemTableModel model = new ItemTableModel();
    private final JTable table = new JTable(model);

    public FavoritesPanel(UiContext context) {
        super(new BorderLayout(8, 8));
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton removeButton = new JButton("取消收藏");
        JButton refreshButton = new JButton("刷新");
        toolbar.add(removeButton);
        toolbar.add(refreshButton);
        JLabel tip = new JLabel("收藏的物品下架后仍会保留在这里");
        tip.setForeground(Color.GRAY);
        toolbar.add(tip);
        add(toolbar, BorderLayout.NORTH);

        table.setRowHeight(26);
        add(new JScrollPane(table), BorderLayout.CENTER);

        removeButton.addActionListener(e -> removeFavorite());
        refreshButton.addActionListener(e -> refresh());

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
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UiUtils.showInfo(this, "请先选择要取消收藏的物品");
            return;
        }
        Item item = model.getItemAt(table.convertRowIndexToModel(viewRow));
        try {
            context.itemService().unfavorite(context.currentUserId(), item.getId());
            refresh();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }
}
