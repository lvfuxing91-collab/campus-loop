package edu.campusloop.core.logging;

import edu.campusloop.core.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {

    private static final String ROOT_NAME = "edu.campusloop";
    private static boolean initialized;

    private AppLogger() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        try {
            Path logDir = AppConfig.getInstance().getLogDirectory();
            Files.createDirectories(logDir);
            FileHandler handler = new FileHandler(logDir.resolve("campus-loop.log").toString(), 1024 * 1024, 3, true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new SimpleFormatter());
            Logger root = Logger.getLogger(ROOT_NAME);
            root.addHandler(handler);
            root.setLevel(Level.INFO);
        } catch (IOException e) {
            Logger.getLogger(ROOT_NAME).log(Level.WARNING, "无法写入日志文件，仅输出到控制台", e);
        }
        initialized = true;
    }

    public static Logger get(Class<?> type) {
        return Logger.getLogger(ROOT_NAME + "." + type.getSimpleName());
    }
}
