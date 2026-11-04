package edu.campusloop.ui.dialog;

import edu.campusloop.core.enums.ItemCondition;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.Item;
import edu.campusloop.ui.UiContext;
import edu.campusloop.ui.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PublishItemDialog extends JDialog {

    private final UiContext context;
    private final Item existing;

    private final JTextField titleField = new JTextField(20);
    private final JComboBox<Category> categoryCombo = new JComboBox<>();
    private final JComboBox<ItemCondition> conditionCombo = new JComboBox<>(ItemCondition.values());
    private final JComboBox<TradeType> tradeCombo = new JComboBox<>(TradeType.values());
    private final JTextField priceField = new JTextField(10);
    private final JTextField tagsField = new JTextField(20);
    private final JTextArea descriptionArea = new JTextArea(5, 20);

    public PublishItemDialog(Window owner, UiContext context, Item existing) {
        super(owner, existing == null ? "发布闲置物品" : "编辑物品", Dialog.ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.existing = existing;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(createForm(), BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 10);

        addRow(panel, gbc, "物品名称", titleField);
        addRow(panel, gbc, "分类", categoryCombo);
        addRow(panel, gbc, "成色", conditionCombo);
        addRow(panel, gbc, "交易方式", tradeCombo);
        addRow(panel, gbc, "价格（元）", priceField);
        addRow(panel, gbc, "标签（逗号分隔）", tagsField);

        gbc.gridx = 0;
        gbc.gridwidth = 1;
        panel.add(new JLabel("物品描述"), gbc);
        gbc.gridx = 1;
        JScrollPane scroll = new JScrollPane(descriptionArea);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        panel.add(scroll, gbc);
        gbc.gridy++;

        JButton save = new JButton(existing == null ? "发布" : "保存");
        JButton cancel = new JButton("取消");
        JPanel buttons = new JPanel();
        buttons.add(save);
        buttons.add(cancel);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(12, 0, 0, 0);
        panel.add(buttons, gbc);

        loadCategories();
        if (existing != null) {
            fillFrom(existing);
        }
        priceField.setText(existing == null ? "0" : existing.getPrice().stripTrailingZeros().toPlainString());

        save.addActionListener(e -> save());
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

    private void loadCategories() {
        try {
            for (Category category : context.itemService().categories()) {
                categoryCombo.addItem(category);
            }
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private void fillFrom(Item item) {
        titleField.setText(item.getTitle());
        descriptionArea.setText(item.getDescription());
        conditionCombo.setSelectedItem(item.getCondition());
        tradeCombo.setSelectedItem(item.getTradeType());
        tagsField.setText(String.join(",", item.getTags()));
        for (int i = 0; i < categoryCombo.getItemCount(); i++) {
            if (categoryCombo.getItemAt(i).getId() == item.getCategoryId()) {
                categoryCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void save() {
        Category category = (Category) categoryCombo.getSelectedItem();
        if (category == null) {
            UiUtils.showInfo(this, "请选择物品分类");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText().trim());
        } catch (NumberFormatException e) {
            UiUtils.showInfo(this, "价格必须是数字");
            return;
        }

        Item draft = new Item();
        draft.setCategoryId(category.getId());
        draft.setTitle(titleField.getText());
        draft.setDescription(descriptionArea.getText());
        draft.setCondition((ItemCondition) conditionCombo.getSelectedItem());
        draft.setTradeType((TradeType) tradeCombo.getSelectedItem());
        draft.setPrice(price);
        draft.setTags(parseTags(tagsField.getText()));

        try {
            if (existing == null) {
                context.itemService().publish(context.currentUserId(), draft);
                UiUtils.showInfo(this, "发布成功");
            } else {
                context.itemService().update(context.currentUserId(), existing.getId(), draft);
                UiUtils.showInfo(this, "保存成功");
            }
            dispose();
        } catch (RuntimeException e) {
            UiUtils.showError(this, e);
        }
    }

    private List<String> parseTags(String raw) {
        List<String> tags = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return tags;
        }
        for (String piece : raw.split("[,，]")) {
            String tag = piece.trim();
            if (!tag.isEmpty() && !tags.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }
}
