/*
 * Onyix Client - Breeze-style category tab button.
 * Pill-style active indicator with cyan underline.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WCategoryTab extends WPressable implements OnyixWidget {
    public final String name;
    public boolean active;

    public WCategoryTab(String name) {
        this.name = name;
        this.active = false;
    }

    @Override
    public double pad() { return 0; }

    @Override
    protected void onCalculateSize() {
        double padH = theme.scale(12);
        double padV = theme.scale(5);
        width  = padH + theme.textWidth(name) + padH;
        height = padV + theme.textHeight() + padV + theme.scale(2); // +2 for underline space
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        OnyixGuiTheme t = theme();

        // Background
        Color bg;
        if (active)          bg = new Color(0, 188, 212, 25);
        else if (mouseOver)  bg = new Color(55, 55, 55, 180);
        else                 bg = new Color(0, 0, 0, 0);
        renderer.quad(x, y, width, height, bg);

        // Cyan underline when active
        if (active) {
            double lineH = t.scale(2);
            renderer.quad(x + t.scale(4), y + height - lineH, width - t.scale(8), lineH, t.accentColor.get());
        }

        // Label
        double textX = x + (width - t.textWidth(name)) / 2.0;
        double textY = y + t.scale(5);
        Color textColor = active ? t.accentColor.get() : (mouseOver ? new Color(210, 210, 210) : new Color(160, 160, 160));
        renderer.text(name, textX, textY, textColor, false);
    }
}
