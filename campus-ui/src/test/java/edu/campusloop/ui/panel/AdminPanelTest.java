package edu.campusloop.ui.panel;

import edu.campusloop.core.model.Category;
import edu.campusloop.core.model.StatsReport;
import edu.campusloop.core.model.User;
import edu.campusloop.core.enums.Role;
import edu.campusloop.core.service.AdminService;
import edu.campusloop.ui.UiContext;
import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AdminPanelTest {

    @Test
    public void selectingAndSavingCategoryUpdatesAllEditableFields() throws Exception {
        FakeAdminService service = new FakeAdminService(category(true));
        AtomicReference<AdminPanel> panelRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> panelRef.set(new AdminPanel(context(service))));
        AdminPanel panel = panelRef.get();
        SwingUtilities.invokeAndWait(() -> {
            JList<?> list = named(panel, "admin.categoryList", JList.class);
            list.setSelectedIndex(0);
            JTextField name = named(panel, "admin.categoryName", JTextField.class);
            JTextField sortOrder = named(panel, "admin.categorySortOrder", JTextField.class);
            JCheckBox enabled = named(panel, "admin.categoryEnabled", JCheckBox.class);
            assertEquals("图书", name.getText());
            assertEquals("10", sortOrder.getText());

            name.setText("教材");
            sortOrder.setText("20");
            enabled.setSelected(false);
            named(panel, "admin.saveCategory", AbstractButton.class).doClick();
        });

        assertNotNull(service.saved);
        assertEquals(7L, service.saved.getId());
        assertEquals("教材", service.saved.getName());
        assertEquals(20, service.saved.getSortOrder());
        assertFalse(service.saved.isEnabled());
    }

    @Test
    public void toggleCategoryPersistsOppositeEnabledState() throws Exception {
        FakeAdminService service = new FakeAdminService(category(true));
        AtomicReference<AdminPanel> panelRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            AdminPanel panel = new AdminPanel(context(service));
            panelRef.set(panel);
            named(panel, "admin.categoryList", JList.class).setSelectedIndex(0);
            named(panel, "admin.toggleCategory", AbstractButton.class).doClick();
        });

        assertNotNull(panelRef.get());
        assertNotNull(service.saved);
        assertFalse(service.saved.isEnabled());
    }

    private UiContext context(AdminService adminService) {
        UiContext context = new UiContext(null, null, null, null, null, adminService);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        context.setCurrentUser(admin);
        return context;
    }

    private Category category(boolean enabled) {
        Category category = new Category();
        category.setId(7L);
        category.setName("图书");
        category.setSortOrder(10);
        category.setEnabled(enabled);
        return category;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName()) && type.isInstance(component)) {
                return (T) component;
            }
            if (component instanceof Container) {
                T nested = named((Container) component, name, type);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static final class FakeAdminService implements AdminService {
        private final Category category;
        private Category saved;

        private FakeAdminService(Category category) {
            this.category = category;
        }

        @Override
        public List<User> listUsers() {
            return Collections.emptyList();
        }

        @Override
        public void setUserEnabled(long adminId, long userId, boolean enabled) {
        }

        @Override
        public void takeDownItem(long adminId, long itemId, String reason) {
        }

        @Override
        public List<Category> listCategories() {
            return Collections.singletonList(category);
        }

        @Override
        public void saveCategory(long adminId, Category category) {
            saved = category;
        }

        @Override
        public StatsReport platformStatistics(long adminId) {
            return null;
        }
    }
}
