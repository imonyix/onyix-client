/*
 * Onyix Client - Breeze-style module card widget.
 *
 * Each card shows:
 *   - Dark panel background (#2d2d2d / #363636 when active)
 *   - Cyan left accent bar (3 px) when module is active
 *   - Module name (white / cyan when active)
 *   - Short description in secondary gray
 *   - Small toggle indicator circle (top-right corner)
 *
 * Left-click  -> toggle module
 * Right-click -> open module settings screen
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WModuleCard extends WPressable implements OnyixWidget {
    private final Module module;

    private double animProgress; // 0..1 for active state animation

    public WModuleCard(Module module) {
        this.module = module;
        this.tooltip = module.description;
        this.animProgress = module.isActive() ? 1.0 : 0.0;
    }

    @Override
    public double pad() { return 0; }

    @Override
    protected void onCalculateSize() {
        // Size is driven externally by WModuleGrid; provide a sensible default.
        double padH = theme.scale(10);
        double padV = theme.scale(8);
        if (width == 0)
            width = padH * 2 + Math.max(theme.textWidth(module.title), theme.textWidth(module.description));
        if (height == 0)
            height = padV * 2 + theme.textHeight() + theme.scale(4) + theme.textHeight() * 0.75;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT)  module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        OnyixGuiTheme t = theme();

        // Animate active bar
        animProgress += delta * 7.0 * (module.isActive() ? 1 : -1);
        animProgress = Mth.clamp(animProgress, 0, 1);

        boolean active  = module.isActive();
        boolean hovered = mouseOver;

        // ── Card background ──────────────────────────────────────────────
        Color bg;
        if (active && hovered)      bg = new Color(40, 55, 58, 245);
        else if (active)            bg = new Color(32, 48, 52, 240);
        else if (hovered)           bg = new Color(55, 55, 55, 230);
        else                        bg = new Color(42, 42, 42, 220);
        renderer.quad(x, y, width, height, bg);

        // ── Subtle top border ────────────────────────────────────────────
        Color topBorder = active ? t.accentColor.get() : new Color(65, 65, 65, 180);
        renderer.quad(x, y, width, t.scale(1), topBorder);

        // ── Left accent bar (animated) ───────────────────────────────────
        if (animProgress > 0) {
            double barW  = t.scale(3);
            double barH  = height * animProgress;
            double barY  = y + (height - barH) / 2.0;
            renderer.quad(x, barY, barW, barH, t.accentColor.get());
        }

        // ── Toggle indicator (top-right circle) ─────────────────────────
        double dotR  = t.scale(4);
        double dotX  = x + width - dotR * 2 - t.scale(6);
        double dotY  = y + t.scale(6);
        Color dotColor = active ? t.accentColor.get() : new Color(80, 80, 80, 200);
        renderer.quad(dotX, dotY, dotR * 2, dotR * 2, dotColor);

        // ── Module name ──────────────────────────────────────────────────
        double padH  = t.scale(10);
        double padV  = t.scale(9);
        Color nameColor = active ? t.accentColor.get() : new Color(220, 220, 220);
        renderer.text(module.title, x + padH, y + padV, nameColor, false);

        // ── Description ──────────────────────────────────────────────────
        double descY = y + padV + t.textHeight() + t.scale(4);
        // Truncate description to fit card width
        String desc = module.description;
        double maxDescW = width - padH * 2 - dotR * 2 - t.scale(8);
        while (desc.length() > 3 && t.textWidth(desc) > maxDescW) {
            desc = desc.substring(0, desc.length() - 1);
        }
        if (!desc.equals(module.description)) desc = desc.substring(0, Math.max(0, desc.length() - 3)) + "...";
        renderer.text(desc, x + padH, descY, new Color(130, 130, 130), false);
    }
}
