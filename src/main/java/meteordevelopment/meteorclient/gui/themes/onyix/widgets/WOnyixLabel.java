/*
 * Onyix Client - Breeze-style label widget.
 * Title labels render in cyan accent, regular labels in light gray.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WOnyixLabel extends WLabel implements OnyixWidget {
    public WOnyixLabel(String text, boolean title) {
        super(text, title);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!text.isEmpty()) {
            OnyixGuiTheme theme = theme();
            Color renderColor;
            if (color != null) {
                renderColor = color;
            } else if (title) {
                renderColor = theme.accentColor.get();
            } else {
                renderColor = theme.textColor.get();
            }
            renderer.text(text, x, y, renderColor, title);
        }
    }
}
