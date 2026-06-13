/*
 * Onyix Client - Breeze-style widget base interface.
 * Renders with dark theme (#1a1a1a bg, #2d2d2d panels) and cyan accent (#00bcd4).
 */

package meteordevelopment.meteorclient.gui.themes.onyix;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.BaseWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

public interface OnyixWidget extends BaseWidget {
    default OnyixGuiTheme theme() {
        return (OnyixGuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, Color outlineColor, Color backgroundColor) {
        OnyixGuiTheme theme = theme();
        double s = theme.scale(2);
        double r = theme.scale(4); // 4px border radius for Breeze style

        // Main background fill
        renderer.quad(widget.x + s, widget.y + s, widget.width - s * 2, widget.height - s * 2, backgroundColor);

        // Outline borders (top, bottom, left, right)
        renderer.quad(widget.x, widget.y, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + widget.height - s, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + s, s, widget.height - s * 2, outlineColor);
        renderer.quad(widget.x + widget.width - s, widget.y + s, s, widget.height - s * 2, outlineColor);

        // Corner fills
        renderer.quad(widget.x, widget.y, s, s, outlineColor);
        renderer.quad(widget.x + widget.width - s, widget.y, s, s, outlineColor);
        renderer.quad(widget.x, widget.y + widget.height - s, s, s, outlineColor);
        renderer.quad(widget.x + widget.width - s, widget.y + widget.height - s, s, s, outlineColor);
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        OnyixGuiTheme theme = theme();
        renderBackground(renderer, widget, theme.outlineColor.get(pressed, mouseOver), theme.backgroundColor.get(pressed, mouseOver));
    }

    /**
     * Render a card background for module cards.
     * Active cards have a subtle cyan tint; inactive cards are dark gray.
     */
    default void renderCardBackground(GuiRenderer renderer, WWidget widget, boolean active) {
        OnyixGuiTheme theme = theme();
        Color bg = active ? theme.moduleBackground.get() : new Color(45, 45, 45, 220);
        Color border = active ? theme.accentColor.get() : new Color(60, 60, 60);
        renderBackground(renderer, widget, border, bg);
    }

    /**
     * Render a full-width panel background (for sidebar, top bar, content area).
     */
    default void renderPanelBackground(GuiRenderer renderer, WWidget widget, Color color) {
        renderer.quad(widget.x, widget.y, widget.width, widget.height, color);
    }
}
