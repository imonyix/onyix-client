/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;

public class WOnyixTriangle extends WTriangle implements OnyixWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.rotatedQuad(x, y, width, height, rotation, GuiRenderer.TRIANGLE, theme().backgroundColor.get(pressed, mouseOver));
    }
}
