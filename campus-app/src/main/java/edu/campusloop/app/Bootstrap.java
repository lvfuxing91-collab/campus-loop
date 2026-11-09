package edu.campusloop.app;

import edu.campusloop.core.config.AppConfig;
import edu.campusloop.dao.DaoFactory;
import edu.campusloop.dao.db.DatabaseManager;
import edu.campusloop.dao.db.SchemaInitializer;
import edu.campusloop.service.ServiceFactory;
import edu.campusloop.service.TransactionRunner;
import edu.campusloop.ui.LoginFrame;
import edu.campusloop.ui.UiContext;

public final class Bootstrap {

    private Bootstrap() {
    }

    public static void start() {
        DatabaseManager database = DatabaseManager.initialize(AppConfig.getInstance().getDatabasePath());
        SchemaInitializer.initialize(database);

        DaoFactory daos = new DaoFactory(database);
        TransactionRunner transactions = work ->
                database.inTransaction(connection -> {
                    work.run();
                    return null;
                });
        ServiceFactory services = new ServiceFactory(
                daos.userRepository(),
                daos.categoryRepository(),
                daos.itemRepository(),
                daos.favoriteRepository(),
                daos.dealRepository(),
                daos.reviewRepository(),
                daos.operationLogRepository(),
                daos.carbonFactorRepository(),
                transactions);

        UiContext uiContext = new UiContext(
                services.authService(),
                services.itemService(),
                services.statsService(),
                services.adminService());
        new LoginFrame(uiContext).setVisible(true);
    }
}
