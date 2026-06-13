/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets.pressable;

import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WOnyixFavorite extends WFavorite implements OnyixWidget {
    public WOnyixFavorite(boolean checked) {
        super(checked);
    }

    @Override
    protected Color getColor() {
        return theme().accentColor.get();
    }
}
