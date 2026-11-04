package edu.campusloop.core.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private final Path home;
    private final Path databasePath;
    private final Path imageDirectory;
    private final Path logDirectory;

    private AppConfig() {
        String base = System.getProperty("campusloop.home", System.getProperty("user.dir", "."));
        home = Paths.get(base).toAbsolutePath().normalize();
        String dbOverride = System.getProperty("campusloop.db");
        databasePath = dbOverride == null
                ? home.resolve("data").resolve("campus.db")
                : Paths.get(dbOverride).toAbsolutePath().normalize();
        imageDirectory = home.resolve("data").resolve("images");
        logDirectory = home.resolve("logs");
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public Path getHome() {
        return home;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public Path getImageDirectory() {
        return imageDirectory;
    }

    public Path getLogDirectory() {
        return logDirectory;
    }
}
