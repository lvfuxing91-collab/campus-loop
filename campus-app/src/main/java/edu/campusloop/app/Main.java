package edu.campusloop.app;

import edu.campusloop.core.logging.AppLogger;
import edu.campusloop.ui.UiUtils;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.logging.Level;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        AppLogger.init();
        SwingUtilities.invokeLater(() -> {
            UiUtils.applyLookAndFeel();
            try {
                Bootstrap.start();
            } catch (Throwable error) {
                AppLogger.get(Main.class).log(Level.SEVERE, "系统启动失败", error);
                String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                JOptionPane.showMessageDialog(null, "系统启动失败：\n" + message,
                        "启动失败", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
