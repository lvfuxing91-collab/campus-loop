package edu.campusloop.ui;

import edu.campusloop.core.config.AppConfig;
import edu.campusloop.core.model.User;
import edu.campusloop.ui.panel.AdminPanel;
import edu.campusloop.ui.panel.BrowsePanel;
import edu.campusloop.ui.panel.DealPanel;
import edu.campusloop.ui.panel.FavoritesPanel;
import edu.campusloop.ui.panel.MyItemsPanel;
import edu.campusloop.ui.panel.StatsPanel;
import edu.campusloop.ui.dialog.ChangePasswordDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class MainFrame extends JFrame {

    private final UiContext context;
    private final BrowsePanel browsePanel;
    private final MyItemsPanel myItemsPanel;
    private final FavoritesPanel favoritesPanel;
    private final DealPanel dealPanel;
    private final StatsPanel statsPanel;
    private final AdminPanel adminPanel;

    public MainFrame(UiContext context) {
        super("低碳校园资源循环流转系统");
        this.context = context;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
        setJMenuBar(createMenuBar());
        add(createHeader(), BorderLayout.NORTH);

        browsePanel = new BrowsePanel(context);
        myItemsPanel = new MyItemsPanel(context);
        favoritesPanel = new FavoritesPanel(context);
        dealPanel = new DealPanel(context);
        statsPanel = new StatsPanel(context);
        adminPanel = context.getCurrentUser().isAdmin() ? new AdminPanel(context) : null;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("物品检索", browsePanel);
        tabs.addTab("我的发布", myItemsPanel);
        tabs.addTab("我的收藏", favoritesPanel);
        tabs.addTab("流转记录", dealPanel);
        tabs.addTab("统计报表", statsPanel);
        if (adminPanel != null) {
            tabs.addTab("管理员后台", adminPanel);
        }
        tabs.addChangeListener(e -> {
            java.awt.Component selected = tabs.getSelectedComponent();
            if (selected == browsePanel) {
                browsePanel.refresh();
            } else if (selected == myItemsPanel) {
                myItemsPanel.refresh();
            } else if (selected == favoritesPanel) {
                favoritesPanel.refresh();
            } else if (selected == dealPanel) {
                dealPanel.refresh();
            } else if (selected == statsPanel) {
                statsPanel.refresh();
            } else if (selected == adminPanel) {
                adminPanel.refresh();
            }
        });
        add(tabs, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("文件");
        JMenuItem exit = new JMenuItem("退出");
        exit.addActionListener(e -> System.exit(0));
        fileMenu.add(exit);

        JMenu accountMenu = new JMenu("账号");
        JMenuItem changePassword = new JMenuItem("修改密码");
        changePassword.addActionListener(e -> new ChangePasswordDialog(this, context).setVisible(true));
        JMenuItem switchAccount = new JMenuItem("切换账号");
        switchAccount.addActionListener(e -> switchAccount());
        accountMenu.add(changePassword);
        accountMenu.add(switchAccount);

        JMenu helpMenu = new JMenu("帮助");
        JMenuItem about = new JMenuItem("关于");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "低碳校园资源循环流转系统\n版本 1.0.0-SNAPSHOT\n《软件构造》分组实验\n\n"
                        + "第三组：成员1（数据层） / 成员2（业务与测试） / 成员3（GUI 与交付）",
                "关于", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(about);

        bar.add(fileMenu);
        bar.add(accountMenu);
        bar.add(helpMenu);
        return bar;
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(0xEE, 0xF6, 0xF1));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        User user = context.getCurrentUser();
        JLabel label = new JLabel("当前用户：" + user.getNickname()
                + "（" + user.getRole().getDisplayName() + "）    信用分：" + user.getCreditScore());
        label.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));

        JButton switchUser = new JButton("切换账号");
        switchUser.addActionListener(e -> switchAccount());

        panel.add(label, BorderLayout.WEST);
        panel.add(switchUser, BorderLayout.EAST);
        return panel;
    }

    private void switchAccount() {
        context.setCurrentUser(null);
        dispose();
        new LoginFrame(context).setVisible(true);
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        JLabel label = new JLabel("数据存储：" + AppConfig.getInstance().getDatabasePath());
        label.setForeground(Color.GRAY);
        panel.add(label, BorderLayout.WEST);
        return panel;
    }
}
