/*
 * Onyix Client - Breeze-style top bar.
 * Dark background with cyan brand name.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WOnyixTopBar extends WTopBar implements OnyixWidget {
    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        if (pressed) return new Color(35, 35, 35, 255);
        if (hovered) return new Color(48, 48, 48, 255);
        return new Color(28, 28, 28, 255);
    }

    @Override
    protected Color getNameColor() {
        return theme().accentColor.get();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        // Dark top bar background
        renderer.quad(x, y, width, height, new Color(22, 22, 22, 255));
        // Bottom separator line
        renderer.quad(x, y + height - theme().scale(1), width, theme().scale(1), new Color(50, 50, 50, 255));
        super.onRender(renderer, mouseX, mouseY, delta);
    }
}
