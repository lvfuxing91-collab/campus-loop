package edu.campusloop.ui.panel;

import edu.campusloop.core.model.StatsReport;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Map;

public class StatsPanel extends JPanel {

    private final UiContext context;
    private final JLabel totalUsersValue = new JLabel("-");
    private final JLabel totalItemsValue = new JLabel("-");
    private final JLabel onSaleValue = new JLabel("-");
    private final JLabel completedDealsValue = new JLabel("-");
    private final JLabel carbonValue = new JLabel("-");
    private final DefaultTableModel categoryModel = new DefaultTableModel(
            new Object[]{"分类", "物品数量"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public StatsPanel(UiContext context) {
        super(new BorderLayout(12, 12));
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("平台数据统计");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton refreshButton = new JButton("刷新统计");
        JButton exportButton = new JButton("导出 CSV（迭代 2）");
        exportButton.setEnabled(false);
        exportButton.setToolTipText("统计报表导出将在迭代 2 提供");
        actions.add(exportButton);
        actions.add(refreshButton);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 5, 12, 0));
        cards.add(createCard("用户总数", totalUsersValue));
        cards.add(createCard("物品总数", totalItemsValue));
        cards.add(createCard("在售物品", onSaleValue));
        cards.add(createCard("完成流转", completedDealsValue));
        cards.add(createCard("低碳减排估算(kg)", carbonValue));

        JTable categoryTable = new JTable(categoryModel);
        categoryTable.setRowHeight(26);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.add(cards, BorderLayout.NORTH);
        body.add(new JScrollPane(categoryTable), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JLabel note = new JLabel("说明：流转数量与 CSV 导出将在接入流转记录后启用；减排量按各分类系数估算。");
        note.setForeground(Color.GRAY);
        add(note, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> refresh());
        refresh();
    }

    private JPanel createCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDD, 0xDD, 0xDD)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        valueLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        return card;
    }

    public void refresh() {
        try {
            StatsReport report = context.statsService().generate(context.currentUserId());
            totalUsersValue.setText(String.valueOf(report.getTotalUsers()));
            totalItemsValue.setText(String.valueOf(report.getTotalItems()));
            onSaleValue.setText(String.valueOf(report.getOnSaleItems()));
            completedDealsValue.setText(String.valueOf(report.getCompletedDeals()));
            carbonValue.setText(String.valueOf(report.getCarbonSavedKg()));
            categoryModel.setRowCount(0);
            for (Map.Entry<String, Integer> entry : report.getItemsByCategory().entrySet()) {
                categoryModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }
}
