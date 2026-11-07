package edu.campusloop.ui;

import edu.campusloop.core.exception.AppException;
import edu.campusloop.core.logging.AppLogger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Component;
import java.awt.Font;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UiUtils {

    private static final Logger LOG = AppLogger.get(UiUtils.class);

    private UiUtils() {
    }

    public static void applyLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Nimbus 主题不可用，使用默认主题", e);
        }
        FontUIResource font = new FontUIResource(new Font("Microsoft YaHei", Font.PLAIN, 13));
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key.toString().endsWith(".font")) {
                UIManager.put(key, font);
            }
        }
    }

    /**
     * 展示错误提示。
     * <p>
     * 只把可安全展示的中文消息弹给用户；数据库SQL、文件路径等诊断细节
     * 通过 {@link AppException#getDetail()} 取得并只写入日志。
     */
    public static void showError(Component parent, Throwable error) {
        String detail = null;
        if (error instanceof AppException) {
            detail = ((AppException) error).getDetail();
        }
        if (detail != null && !detail.trim().isEmpty()) {
            LOG.log(Level.WARNING, "界面操作失败：" + detail, error);
        } else {
            LOG.log(Level.WARNING, "界面操作失败", error);
        }
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "操作失败，请稍后重试";
        }
        JOptionPane.showMessageDialog(parent, message, "操作失败", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "确认操作",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
