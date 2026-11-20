package ua.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;
import ua.client.module.Category;
import ua.client.module.Module;
import ua.client.module.ModuleManager;
import ua.client.module.Setting;

public class ClickGui extends Screen {
    private static class Hit {
        int x1, y1, x2, y2;
        Module m;
        Setting s;
    }

    private final List<Hit> hits = new ArrayList<>();
    private Module listening;

    public ClickGui() { super(new LiteralText("PastaClient")); }

    @Override public boolean isPauseScreen() { return false; }

    private void addHit(int x1, int y1, int x2, int y2, Module m, Setting s) {
        Hit h = new Hit();
        h.x1 = x1; h.y1 = y1; h.x2 = x2; h.y2 = y2; h.m = m; h.s = s;
        hits.add(h);
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        renderBackground(ms);
        hits.clear();
        int x = 12, w = 112;
        for (Category c : Category.values()) {
            int y = 12;
            fill(ms, x, y, x + w, y + 16, 0xFF6A2BD9);
            drawStringWithShadow(ms, textRenderer, c.label, x + 4, y + 4, 0xFFFFFFFF);
            y += 16;
            for (Module m : ModuleManager.in(c)) {
                boolean hover = mx >= x && mx < x + w && my >= y && my < y + 14;
                int col = m.enabled ? (hover ? 0xEE8F55F0 : 0xEE7A3BE8) : (hover ? 0xEE2A2A36 : 0xEE1A1A22);
                fill(ms, x, y, x + w, y + 14, col);
                String label = m.name;
                if (listening == m) label += " [...]";
                else if (m.key >= 0) label += " [" + InputUtil.fromKeyCode(m.key, -1).getLocalizedText().getString() + "]";
                drawStringWithShadow(ms, textRenderer, label, x + 4, y + 3, 0xFFFFFFFF);
                addHit(x, y, x + w, y + 14, m, null);
                y += 14;
                if (m.expanded) {
                    for (Setting s : m.settings) {
                        fill(ms, x, y, x + w, y + 12, 0xDD101015);
                        drawStringWithShadow(ms, textRenderer, s.name + ": " + s.display(), x + 8, y + 2, 0xFFB8B8B8);
                        addHit(x, y, x + w, y + 12, m, s);
                        y += 12;
                    }
                }
            }
            x += w + 6;
        }
        drawStringWithShadow(ms, textRenderer, "LMB toggle | RMB settings | MMB bind key | settings: LMB +, RMB -",
                12, height - 14, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Hit h : hits) {
            if (mx < h.x1 || mx >= h.x2 || my < h.y1 || my >= h.y2) continue;
            if (h.s != null) {
                h.s.change(button != 1);
            } else if (button == 0) {
                h.m.toggle();
            } else if (button == 1) {
                h.m.expanded = !h.m.expanded;
            } else if (button == 2) {
                listening = h.m;
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            listening.key = keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
            listening = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
