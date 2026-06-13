/*
 * Onyix Client - Full-screen dark background overlay.
 * Renders a near-opaque very dark overlay (#0f0f0f) covering the entire
 * screen to create the Breeze-style GUI backdrop.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class WFullBackground extends WWidget implements OnyixWidget {
    @Override
    protected void onCalculateSize() {
        width  = getWindowWidth();
        height = getWindowHeight();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        // Very dark overlay - matches Breeze reference image background
        renderer.quad(0, 0, getWindowWidth(), getWindowHeight(), new Color(15, 15, 15, 252));
    }
}
