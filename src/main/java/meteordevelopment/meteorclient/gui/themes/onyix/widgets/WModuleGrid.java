/*
 * Onyix Client - Multi-column module card grid.
 * Lays out WModuleCard widgets in a responsive grid matching the Breeze
 * reference image (3 columns of equal-width cards).
 */

package meteordevelopment.meteorclient.gui.themes.onyix.widgets;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixGuiTheme;
import meteordevelopment.meteorclient.gui.themes.onyix.OnyixWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom widget that arranges WModuleCard children in a fixed 3-column grid.
 * Each card gets an equal share of the available width.
 */
public class WModuleGrid extends WWidget implements OnyixWidget {
    private static final int COLUMNS = 3;
    private static final double GAP = 4; // gap between cards in unscaled px

    private final GuiTheme theme;
    private final List<WModuleCard> cards = new ArrayList<>();

    public WModuleGrid(GuiTheme theme) {
        this.theme = theme;
    }

    public void setModules(List<Module> modules) {
        cards.clear();
        for (Module m : modules) {
            WModuleCard card = new WModuleCard(m);
            card.theme = theme;
            cards.add(card);
        }
        invalidate();
    }

    @Override
    protected void onCalculateSize() {
        double gap = theme.scale(GAP);
        double cardW = (width - gap * (COLUMNS - 1)) / COLUMNS;
        double cardH = theme.scale(52); // fixed card height

        int rows = (int) Math.ceil((double) cards.size() / COLUMNS);
        height = rows * cardH + Math.max(0, rows - 1) * gap;

        for (int i = 0; i < cards.size(); i++) {
            WModuleCard card = cards.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            card.x = x + col * (cardW + gap);
            card.y = y + row * (cardH + gap);
            card.width = cardW;
            card.height = cardH;
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        for (WModuleCard card : cards) {
            card.render(renderer, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
        for (WModuleCard card : cards) {
            if (card.onMouseClicked(click, doubled)) return true;
        }
        return false;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        for (WModuleCard card : cards) card.invalidate();
    }
}
