/*
 * Onyix Client - Breeze-style view/scroll container.
 * Dark background with subtle scrollbar.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WOnyixView extends WView implements OnyixWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        // Dark background for the scroll area
        renderer.quad(x, y, width, height, new Color(30, 30, 30, 180));

        // Scrollbar
        if (canScroll && hasScrollBar) {
            OnyixGuiTheme theme = theme();
            Color scrollColor = focused ? theme.accentColor.get() :
                (handleMouseOver ? new Color(80, 80, 80, 200) : new Color(50, 50, 50, 200));
            renderer.quad(handleX(), handleY(), handleWidth(), handleHeight(), scrollColor);
        }
    }
}
