/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WOnyixModule extends WPressable implements OnyixWidget {
    private final Module module;
    private final String title;

    private double titleWidth;
    private double animProgress1;
    private double animProgress2;

    public WOnyixModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        if (module.isActive()) {
            animProgress1 = 1;
            animProgress2 = 1;
        } else {
            animProgress1 = 0;
            animProgress2 = 0;
        }
    }

    @Override
    public double pad() {
        return theme.scale(6);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        if (titleWidth == 0) titleWidth = theme.textWidth(title);

        // Card layout: title + description
        double descWidth = theme.textWidth(module.description);
        width = pad + Math.max(titleWidth, descWidth) + pad;
        height = pad + theme.textHeight() + theme.scale(2) + theme.textHeight() * 0.8 + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        OnyixGuiTheme theme = theme();
        double pad = pad();

        animProgress1 += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
        animProgress1 = Mth.clamp(animProgress1, 0, 1);

        animProgress2 += delta * 6 * (module.isActive() ? 1 : -1);
        animProgress2 = Mth.clamp(animProgress2, 0, 1);

        // Card background with cyan accent border when active
        Color bgNormal = new Color(45, 45, 45, 200);
        Color bgActive = new Color(0, 188, 212, 40);
        Color borderNormal = new Color(60, 60, 60);
        Color borderActive = theme.accentColor.get();

        Color bg = module.isActive() ? bgActive : bgNormal;
        Color border = module.isActive() ? borderActive : borderNormal;
        if (mouseOver && !module.isActive()) {
            bg = new Color(55, 55, 55, 200);
            border = new Color(80, 80, 80);
        }

        renderBackground(renderer, this, border, bg);

        // Accent bar on left when active
        if (animProgress2 > 0) {
            renderer.quad(x, y + height * (1 - animProgress2), theme.scale(3), height * animProgress2, theme.accentColor.get());
        }

        // Title text
        double textX = this.x + pad;
        double w = width - pad * 2;

        if (theme.moduleAlignment.get() == AlignmentX.Center) {
            textX += w / 2 - titleWidth / 2;
        } else if (theme.moduleAlignment.get() == AlignmentX.Right) {
            textX += w - titleWidth;
        }

        Color titleColor = module.isActive() ? theme.accentColor.get() : theme.textColor.get();
        renderer.text(title, textX, y + pad, titleColor, false);

        // Description text (secondary color, smaller)
        renderer.text(module.description, this.x + pad, y + pad + theme.textHeight() + theme.scale(2), theme.textSecondaryColor.get(), false);
    }
}
