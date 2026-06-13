/*
 * Onyix Client - Breeze-style sidebar icon button.
 * Square button with Unicode symbol + label text.
 * Active state: cyan left accent bar + cyan text.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WSidebarIcon extends WPressable implements OnyixWidget {
    private final String label;
    private final String symbol; // Unicode symbol string
    public boolean active;

    public WSidebarIcon(String label, String symbol) {
        this.label  = label;
        this.symbol = symbol;
        this.active = false;
    }

    @Override
    public double pad() { return 0; }

    @Override
    protected void onCalculateSize() {
        width  = theme.scale(56);
        height = theme.scale(52);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        OnyixGuiTheme t = theme();

        // Background
        Color bg;
        if (active)          bg = new Color(35, 35, 35, 255);
        else if (mouseOver)  bg = new Color(48, 48, 48, 255);
        else                 bg = new Color(28, 28, 28, 255);
        renderer.quad(x, y, width, height, bg);

        // Active left accent bar
        if (active) {
            renderer.quad(x, y, t.scale(3), height, t.accentColor.get());
        }

        // Symbol (centered horizontally, upper portion)
        double symW  = t.textWidth(symbol);
        double symX  = x + (width - symW) / 2.0;
        double symY  = y + t.scale(10);
        Color symColor = active ? t.accentColor.get() : new Color(190, 190, 190);
        renderer.text(symbol, symX, symY, symColor, false);

        // Label (centered, below symbol)
        double lblW  = t.textWidth(label);
        double lblX  = x + (width - lblW) / 2.0;
        double lblY  = y + t.scale(28);
        Color lblColor = active ? t.accentColor.get() : new Color(130, 130, 130);
        renderer.text(label, lblX, lblY, lblColor, false);
    }
}
