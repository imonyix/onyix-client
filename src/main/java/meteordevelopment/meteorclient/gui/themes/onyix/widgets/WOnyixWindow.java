/*
 * Onyix Client - Breeze-style window widget.
 * Dark panel with cyan header bar, clean modern look.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WOnyixWindow extends WWindow implements OnyixWidget {
    public WOnyixWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WOnyixHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            // Dark panel body (#2d2d2d)
            renderer.quad(x, y + header.height, width, height - header.height, new Color(45, 45, 45, 240));
        }
    }

    private class WOnyixHeader extends WHeader {
        public WOnyixHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            // Header bar with slight cyan tint background
            renderer.quad(this, new Color(35, 35, 35, 255));
            // Bottom accent line
            renderer.quad(x, y + height - theme().scale(1), width, theme().scale(1), theme().accentColor.get());
        }
    }
}
