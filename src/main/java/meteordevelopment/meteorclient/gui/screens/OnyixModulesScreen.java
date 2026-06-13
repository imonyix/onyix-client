/*
 * Onyix Client - Breeze-style Modules Screen.
 *
 * Full-screen layout matching the Breeze 1.2 reference image:
 * - Dark full-screen background overlay
 * - Left sidebar: navigation icons (Modules, Configs, Socials, Themes, Settings, Scripting)
 * - Right panel:
 *   - Category tabs row
 *   - Search bar
 *   - Multi-column scrollable module card grid
 *   - Bottom watermark
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.WCategoryTab;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.WModuleCard;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.WModuleGrid;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.WSidebarIcon;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.WFullBackground;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.*;

public class OnyixModulesScreen extends TabScreen {
    private String searchFilter = "";
    private Category selectedCategory = null; // null = All
    private WTextBox searchTextBox;
    private WModuleGrid moduleGrid;
    private List<WCategoryTab> categoryTabs = new ArrayList<>();
    private List<WSidebarIcon> sidebarIcons = new ArrayList<>();

    public OnyixModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        // Full-screen dark background
        WFullBackground bg = new WFullBackground();
        bg.theme = theme;
        add(bg);

        // ── Main horizontal layout: sidebar | content ──────────────────────
        WHorizontalList mainLayout = add(theme.horizontalList()).expandX().expandWidgetY().widget();
        mainLayout.spacing = 0;

        // ── Left Sidebar ─────────────────────────────────────────────────────
        WVerticalList sidebar = mainLayout.add(theme.verticalList()).pad(0).widget();
        sidebar.spacing = 0;

        String[] sidebarNames   = {"Modules", "Configs", "Socials", "Themes", "Settings", "Script"};
        String[] sidebarSymbols = {"\u2630", "\u2699", "\u2665", "\u2605", "\u2692", "\u270E"};

        sidebarIcons.clear();
        for (int i = 0; i < sidebarNames.length; i++) {
            WSidebarIcon icon = new WSidebarIcon(sidebarNames[i], sidebarSymbols[i]);
            if (i == 0) icon.active = true;

            final int idx = i;
            icon.action = () -> {
                for (WSidebarIcon si : sidebarIcons) si.active = false;
                sidebarIcons.get(idx).active = true;

                if (idx == 0) {
                    selectedCategory = null;
                    searchFilter = "";
                    if (searchTextBox != null) searchTextBox.set("");
                    updateCategoryTabActiveStates();
                    refreshModules();
                }
            };

            sidebar.add(icon).pad(0);
            sidebarIcons.add(icon);
        }

        // Vertical separator
        mainLayout.add(theme.verticalSeparator());

        // ── Right Panel ──────────────────────────────────────────────────────
        WVerticalList rightPanel = mainLayout.add(theme.verticalList()).expandX().expandWidgetY().pad(0).widget();
        rightPanel.spacing = 0;

        // Category tabs row
        WHorizontalList tabsRow = rightPanel.add(theme.horizontalList()).padLeft(6).padRight(6).padTop(4).padBottom(4).expandX().widget();
        tabsRow.spacing = 2;

        categoryTabs.clear();
        for (Category category : Modules.loopCategories()) {
            WCategoryTab tab = new WCategoryTab(category.name);
            final Category cat = category;
            tab.action = () -> {
                selectedCategory = cat;
                updateCategoryTabActiveStates();
                refreshModules();
            };
            tabsRow.add(tab).pad(0);
            categoryTabs.add(tab);
        }

        WCategoryTab favoriteTab = new WCategoryTab("Favorite");
        favoriteTab.action = () -> {
            selectedCategory = FAVORITE_CATEGORY;
            updateCategoryTabActiveStates();
            refreshModules();
        };
        tabsRow.add(favoriteTab).pad(0);
        categoryTabs.add(favoriteTab);

        WCategoryTab allTab = new WCategoryTab("All");
        allTab.active = true;
        allTab.action = () -> {
            selectedCategory = null;
            updateCategoryTabActiveStates();
            refreshModules();
        };
        tabsRow.add(allTab).pad(0);
        categoryTabs.add(allTab);

        // Horizontal separator below tabs
        rightPanel.add(theme.horizontalSeparator(null)).expandX();

        // Search bar
        WHorizontalList searchRow = rightPanel.add(theme.horizontalList())
            .padLeft(8).padRight(8).padTop(4).padBottom(4).expandX().widget();
        searchTextBox = searchRow.add(theme.textBox("", "\uD83D\uDD0D  Search modules...")).expandX().widget();
        searchTextBox.action = () -> {
            searchFilter = searchTextBox.get();
            refreshModules();
        };

        // Scrollable module grid
        WView scrollView = rightPanel.add(theme.view()).pad(6).expandX().expandWidgetY().widget();
        scrollView.scrollOnlyWhenMouseOver = false;
        scrollView.hasScrollBar = true;

        moduleGrid = new WModuleGrid(theme);
        scrollView.add(moduleGrid).expandX();

        // Bottom watermark
        rightPanel.add(theme.label("Onyix Client  \u2022  onyix.cc")).right().padRight(10).padBottom(4);

        refreshModules();
    }

    private void updateCategoryTabActiveStates() {
        for (WCategoryTab tab : categoryTabs) tab.active = false;

        if (selectedCategory == null) {
            if (!categoryTabs.isEmpty())
                categoryTabs.get(categoryTabs.size() - 1).active = true;
        } else if (selectedCategory == FAVORITE_CATEGORY) {
            int favIdx = categoryTabs.size() - 2;
            if (favIdx >= 0) categoryTabs.get(favIdx).active = true;
        } else {
            for (WCategoryTab tab : categoryTabs) {
                if (tab.name.equals(selectedCategory.name)) {
                    tab.active = true;
                    break;
                }
            }
        }
    }

    private void refreshModules() {
        if (moduleGrid == null) return;
        moduleGrid.setModules(getFilteredModules());
    }

    private List<Module> getFilteredModules() {
        List<Module> result = new ArrayList<>();
        for (Module module : Modules.get().getAll()) {
            if (Config.get().hiddenModules.get().contains(module)) continue;

            if (selectedCategory != null) {
                if (selectedCategory == FAVORITE_CATEGORY) {
                    if (!module.favorite) continue;
                } else {
                    if (module.category != selectedCategory) continue;
                }
            }

            if (!searchFilter.isEmpty()) {
                if (!module.name.toLowerCase().contains(searchFilter.toLowerCase()) &&
                    !module.description.toLowerCase().contains(searchFilter.toLowerCase())) continue;
            }
            result.add(module);
        }
        result.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name));
        return result;
    }

    @Override
    public boolean keyPressed(KeyEvent value) {
        if (locked) return false;
        if (value.modifiers() == GLFW_MOD_CONTROL && value.key() == GLFW_KEY_F) {
            if (searchTextBox != null) {
                searchTextBox.setFocused(true);
                searchTextBox.setCursorMax();
            }
            return true;
        }
        return super.keyPressed(value);
    }

    @Override public boolean toClipboard()   { return false; }
    @Override public boolean fromClipboard()  { return false; }

    @Override
    public void reload() {
        clear();
        initWidgets();
    }

    private static final Category FAVORITE_CATEGORY = new Category("Favorite", () -> null);
}
