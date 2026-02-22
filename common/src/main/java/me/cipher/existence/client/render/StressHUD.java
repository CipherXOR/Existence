/*package me.cipher.existence.client.render;

import me.cipher.existence.client.ClientStressManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class StressHUD {
    private static final int BATTERY_WIDTH = 12;
    private static final int BATTERY_HEIGHT = 50;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0xFF000000;

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int load = ClientStressManager.getLoad();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int x = screenWidth - BATTERY_WIDTH - 10;
        int y = 10;

        int fillHeight = (int) ((BATTERY_HEIGHT - 2) * (1 - load / 100.0));
        if (fillHeight < 0) fillHeight = 0;
        if (fillHeight > BATTERY_HEIGHT - 2) fillHeight = BATTERY_HEIGHT - 2;

        guiGraphics.fill(x, y, x + BATTERY_WIDTH, y + BATTERY_HEIGHT, BORDER_COLOR);
        guiGraphics.fill(x + 1, y + 1, x + BATTERY_WIDTH - 1, y + BATTERY_HEIGHT - 1, BACKGROUND_COLOR);
        int fillYStart = y + BATTERY_HEIGHT - 1 - fillHeight;
        guiGraphics.fill(x + 1, fillYStart, x + BATTERY_WIDTH - 1, y + BATTERY_HEIGHT - 1, getFillColor(load));
        guiGraphics.fill(x + 4, y - 2, x + BATTERY_WIDTH - 4, y, BORDER_COLOR);
    }

    private static int getFillColor(int load) {
        float hue = 120f * (1 - load / 100f);
        return 0xFF000000 | hsvToRgb(hue, 1f, 1f);
    }

    private static int hsvToRgb(float h, float s, float v) {
        int r, g, b;
        float c = v * s;
        float hp = h / 60f;
        float x = c * (1 - Math.abs((hp % 2) - 1));
        if (hp < 1) {
            r = (int) c;
            g = (int) x;
            b = 0;
        } else if (hp < 2) {
            r = (int) x;
            g = (int) c;
            b = 0;
        } else if (hp < 3) {
            r = 0;
            g = (int) c;
            b = (int) x;
        } else if (hp < 4) {
            r = 0;
            g = (int) x;
            b = (int) c;
        } else if (hp < 5) {
            r = (int) x;
            g = 0;
            b = (int) c;
        } else {
            r = (int) c;
            g = 0;
            b = (int) x;
        }
        float m = v - c;
        r = (int) ((r + m) * 255);
        g = (int) ((g + m) * 255);
        b = (int) ((b + m) * 255);
        return (r << 16) | (g << 8) | b;
    }
}*/