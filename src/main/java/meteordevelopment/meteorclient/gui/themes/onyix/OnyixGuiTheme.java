/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 *
 * OnyixGUI theme - Breeze-style dark theme with cyan/teal accent.
 */

package meteordevelopment.meteorclient.gui.themes.onyix;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.screens.OnyixModulesScreen;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.*;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.input.WOnyixDropdown;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.input.WOnyixSlider;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.input.WOnyixTextBox;
import meteordevelopment.meteorclient.gui.themes.onyix.widgets.pressable.*;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.*;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class OnyixGuiTheme extends GuiTheme {
    // Onyix Brand Colors
    public static final int BG_DARK = 0xFF1A1A1A;         // #1a1a1a
    public static final int BG_PANEL = 0xFF2D2D2D;         // #2d2d2d
    public static final int BG_CARD = 0xFF363636;           // #363636
    public static final int ACCENT_CYAN = 0xFF00BCD4;      // #00bcd4 (ARGB)
    public static final int ACCENT_CYAN_LIGHT = 0xFF00D8E4;// #00d8e4
    public static final int ACCENT_CYAN_DARK = 0xFF008E9C; // #008e9c

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final SettingGroup sgTextColors = settings.createGroup("Text");
    private final SettingGroup sgBackgroundColors = settings.createGroup("Background");
    private final SettingGroup sgOutline = settings.createGroup("Outline");
    private final SettingGroup sgSeparator = settings.createGroup("Separator");
    private final SettingGroup sgScrollbar = settings.createGroup("Scrollbar");
    private final SettingGroup sgSlider = settings.createGroup("Slider");
    private final SettingGroup sgStarscript = settings.createGroup("Starscript");

    // General
    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the GUI.")
        .defaultValue(1.0)
        .min(0.75)
        .sliderRange(0.75, 4)
        .onSliderRelease()
        .onChanged(_ -> {
            if (mc.screen instanceof WidgetScreen widgetScreen) widgetScreen.invalidate();
        })
        .build()
    );

    public final Setting<AlignmentX> moduleAlignment = sgGeneral.add(new EnumSetting.Builder<AlignmentX>()
        .name("module-alignment")
        .description("How module titles are aligned.")
        .defaultValue(AlignmentX.Center)
        .build()
    );

    public final Setting<Boolean> categoryIcons = sgGeneral.add(new BoolSetting.Builder()
        .name("category-icons")
        .description("Adds item icons to module categories.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> hideHUD = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-HUD")
        .description("Hide HUD when in GUI.")
        .defaultValue(false)
        .onChanged(v -> {
            if (mc.screen instanceof WidgetScreen) mc.options.hideGui = v;
        })
        .build()
    );

    // Colors - Onyix Breeze Style (Cyan/Teal accent instead of purple)
    public final Setting<SettingColor> accentColor = color("accent", "Main color of the GUI (Onyix cyan).", new SettingColor(0, 188, 212));
    public final Setting<SettingColor> checkboxColor = color("checkbox", "Color of checkbox.", new SettingColor(0, 188, 212));
    public final Setting<SettingColor> plusColor = color("plus", "Color of plus button.", new SettingColor(0, 230, 118));
    public final Setting<SettingColor> minusColor = color("minus", "Color of minus button.", new SettingColor(255, 82, 82));
    public final Setting<SettingColor> favoriteColor = color("favorite", "Color of checked favorite button.", new SettingColor(0, 188, 212));

    // Text
    public final Setting<SettingColor> textColor = color(sgTextColors, "text", "Color of text.", new SettingColor(224, 224, 224));
    public final Setting<SettingColor> textSecondaryColor = color(sgTextColors, "text-secondary-text", "Color of secondary text.", new SettingColor(160, 160, 160));
    public final Setting<SettingColor> textHighlightColor = color(sgTextColors, "text-highlight", "Color of text highlighting.", new SettingColor(0, 188, 212, 80));
    public final Setting<SettingColor> titleTextColor = color(sgTextColors, "title-text", "Color of title text.", new SettingColor(0, 188, 212));
    public final Setting<SettingColor> loggedInColor = color(sgTextColors, "logged-in-text", "Color of logged in account name.", new SettingColor(0, 230, 118));
    public final Setting<SettingColor> placeholderColor = color(sgTextColors, "placeholder", "Color of placeholder text.", new SettingColor(255, 255, 255, 30));

    // Background - Dark theme (#1a1a1a base, #2d2d2d panels)
    public final ThreeStateColorSetting backgroundColor = new ThreeStateColorSetting(
        sgBackgroundColors,
        "background",
        new SettingColor(26, 26, 26, 220),
        new SettingColor(38, 38, 38, 220),
        new SettingColor(45, 45, 45, 220)
    );

    public final Setting<SettingColor> moduleBackground = color(sgBackgroundColors, "module-background", "Color of module background when active.", new SettingColor(0, 188, 212, 40));

    // Outline
    public final ThreeStateColorSetting outlineColor = new ThreeStateColorSetting(
        sgOutline,
        "outline",
        new SettingColor(0, 0, 0),
        new SettingColor(20, 20, 20),
        new SettingColor(40, 40, 40)
    );

    // Separator
    public final Setting<SettingColor> separatorText = color(sgSeparator, "separator-text", "Color of separator text", new SettingColor(0, 188, 212));
    public final Setting<SettingColor> separatorCenter = color(sgSeparator, "separator-center", "Center color of separators.", new SettingColor(0, 188, 212, 180));
    public final Setting<SettingColor> separatorEdges = color(sgSeparator, "separator-edges", "Color of separator edges.", new SettingColor(0, 188, 212, 60));

    // Scrollbar
    public final ThreeStateColorSetting scrollbarColor = new ThreeStateColorSetting(
        sgScrollbar,
        "Scrollbar",
        new SettingColor(45, 45, 45, 200),
        new SettingColor(55, 55, 55, 200),
        new SettingColor(65, 65, 65, 200)
    );

    // Slider - Cyan accent
    public final ThreeStateColorSetting sliderHandle = new ThreeStateColorSetting(
        sgSlider,
        "slider-handle",
        new SettingColor(0, 188, 212),
        new SettingColor(0, 210, 230),
        new SettingColor(0, 230, 118)
    );

    public final Setting<SettingColor> sliderLeft = color(sgSlider, "slider-left", "Color of slider left part.", new SettingColor(0, 188, 212));
    public final Setting<SettingColor> sliderRight = color(sgSlider, "slider-right", "Color of slider right part.", new SettingColor(45, 45, 45));

    // Starscript
    private final Setting<SettingColor> starscriptText = color(sgStarscript, "starscript-text", "Color of text in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptBraces = color(sgStarscript, "starscript-braces", "Color of braces in Starscript code.", new SettingColor(150, 150, 150));
    private final Setting<SettingColor> starscriptParenthesis = color(sgStarscript, "starscript-parenthesis", "Color of parenthesis in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptDots = color(sgStarscript, "starscript-dots", "Color of dots in starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptCommas = color(sgStarscript, "starscript-commas", "Color of commas in starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptOperators = color(sgStarscript, "starscript-operators", "Color of operators in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptStrings = color(sgStarscript, "starscript-strings", "Color of strings in Starscript code.", new SettingColor(106, 135, 89));
    private final Setting<SettingColor> starscriptNumbers = color(sgStarscript, "starscript-numbers", "Color of numbers in Starscript code.", new SettingColor(104, 141, 187));
    private final Setting<SettingColor> starscriptKeywords = color(sgStarscript, "starscript-keywords", "Color of keywords in Starscript code.", new SettingColor(204, 120, 50));
    private final Setting<SettingColor> starscriptAccessedObjects = color(sgStarscript, "starscript-accessed-objects", "Color of accessed objects (before a dot) in Starscript code.", new SettingColor(152, 118, 170));

    public OnyixGuiTheme() {
        super("Onyix");
        settingsFactory = new DefaultSettingsWidgetFactory(this);
    }

    private Setting<SettingColor> color(SettingGroup group, String name, String description, SettingColor color) {
        return group.add(new ColorSetting.Builder()
            .name(name + "-color")
            .description(description)
            .defaultValue(color)
            .build());
    }

    private Setting<SettingColor> color(String name, String description, SettingColor color) {
        return color(sgColors, name, description, color);
    }

    // Widgets
    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WOnyixWindow(icon, title));
    }

    @Override
    public WLabel label(String text, boolean title, double maxWidth) {
        if (maxWidth == 0 && !text.contains("\n")) return w(new WOnyixLabel(text, title));
        return w(new WOnyixMultiLabel(text, title, maxWidth));
    }

    @Override
    public WHorizontalSeparator horizontalSeparator(String text) {
        return w(new WOnyixHorizontalSeparator(text));
    }

    @Override
    public WVerticalSeparator verticalSeparator() {
        return w(new WOnyixVerticalSeparator());
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WOnyixButton(text, texture));
    }

    @Override
    protected WConfirmedButton confirmedButton(String text, String confirmText, GuiTexture texture) {
        return w(new WOnyixConfirmedButton(text, confirmText, texture));
    }

    @Override
    public WMinus minus() {
        return w(new WOnyixMinus());
    }

    @Override
    public WConfirmedMinus confirmedMinus() {
        return w(new WOnyixConfirmedMinus());
    }

    @Override
    public WPlus plus() {
        return w(new WOnyixPlus());
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WOnyixCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WOnyixSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WOnyixTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WOnyixDropdown<>(values, value));
    }

    @Override
    public WTriangle triangle() {
        return w(new WOnyixTriangle());
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WOnyixTooltip(text));
    }

    @Override
    public WView view() {
        return w(new WOnyixView());
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WOnyixSection(title, expanded, headerWidget));
    }

    @Override
    public WAccount account(WidgetScreen screen, Account<?> account) {
        return w(new WOnyixAccount(screen, account));
    }

    @Override
    public WWidget module(Module module, String title) {
        // Use WModuleCard for Breeze-style cards when in OnyixModulesScreen
        return w(new WModuleCard(module));
    }

    @Override
    public WQuad quad(Color color) {
        return w(new WOnyixQuad(color));
    }

    @Override
    public WTopBar topBar() {
        return w(new WOnyixTopBar());
    }

    @Override
    public WFavorite favorite(boolean checked) {
        return w(new WOnyixFavorite(checked));
    }

    // Colors
    @Override
    public Color textColor() {
        return textColor.get();
    }

    @Override
    public Color textSecondaryColor() {
        return textSecondaryColor.get();
    }

    // Starscript
    @Override
    public Color starscriptTextColor() { return starscriptText.get(); }
    @Override
    public Color starscriptBraceColor() { return starscriptBraces.get(); }
    @Override
    public Color starscriptParenthesisColor() { return starscriptParenthesis.get(); }
    @Override
    public Color starscriptDotColor() { return starscriptDots.get(); }
    @Override
    public Color starscriptCommaColor() { return starscriptCommas.get(); }
    @Override
    public Color starscriptOperatorColor() { return starscriptOperators.get(); }
    @Override
    public Color starscriptStringColor() { return starscriptStrings.get(); }
    @Override
    public Color starscriptNumberColor() { return starscriptNumbers.get(); }
    @Override
    public Color starscriptKeywordColor() { return starscriptKeywords.get(); }
    @Override
    public Color starscriptAccessedObjectColor() { return starscriptAccessedObjects.get(); }

    // Other
    @Override
    public TextRenderer textRenderer() {
        return TextRenderer.get();
    }

    @Override
    public double scale(double value) {
        double scaled = value * scale.get();
        return scaled;
    }

    @Override
    public boolean categoryIcons() {
        return categoryIcons.get();
    }

    @Override
    public boolean hideHUD() {
        return hideHUD.get();
    }

    // Override modules screen to use Onyix Breeze-style full-screen layout
    @Override
    public TabScreen modulesScreen() {
        return new OnyixModulesScreen(this);
    }

    @Override
    public boolean isModulesScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof OnyixModulesScreen;
    }

    public class ThreeStateColorSetting {
        private final Setting<SettingColor> normal, hovered, pressed;

        public ThreeStateColorSetting(SettingGroup group, String name, SettingColor c1, SettingColor c2, SettingColor c3) {
            normal = color(group, name, "Color of " + name + ".", c1);
            hovered = color(group, "hovered-" + name, "Color of " + name + " when hovered.", c2);
            pressed = color(group, "pressed-" + name, "Color of " + name + " when pressed.", c3);
        }

        public SettingColor get() {
            return normal.get();
        }

        public SettingColor get(boolean pressed, boolean hovered, boolean bypassDisableHoverColor) {
            if (pressed) return this.pressed.get();
            return (hovered && (bypassDisableHoverColor || !disableHoverColor)) ? this.hovered.get() : this.normal.get();
        }

        public SettingColor get(boolean pressed, boolean hovered) {
            return get(pressed, hovered, false);
        }
    }
}
