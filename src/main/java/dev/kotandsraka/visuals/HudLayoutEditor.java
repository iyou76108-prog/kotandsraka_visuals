package dev.kotandsraka.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

public final class HudLayoutEditor {
    private static final int TARGET = 0;
    private static final int ARMOR = 1;
    private static final int KEYS = 2;
    private static final int MODULES = 4;
    private static final int EFFECTS = 5;
    private static final int STAT_FPS = 6;
    private static final int STAT_CPS = 7;
    private static final int STAT_PING = 8;
    private static final int STAT_COORDS = 9;
    private static final int STAT_SPEED = 10;
    private static final int STAT_TOTEMS = 11;
    private static final int STAT_CLOCK = 12;
    private static final int STAT_DIRECTION = 13;

    private static int dragging = -1;
    private static int offsetX;
    private static int offsetY;

    private HudLayoutEditor() {}

    public static int keystrokesX(Minecraft mc, VisualConfig cfg) {
        return cfg.keystrokesHudX < 0 ? Math.max(0, mc.getWindow().getGuiScaledWidth() - 82) : cfg.keystrokesHudX;
    }

    public static int keystrokesY(Minecraft mc, VisualConfig cfg) {
        return cfg.keystrokesHudY < 0 ? Math.max(0, mc.getWindow().getGuiScaledHeight() - 70) : cfg.keystrokesHudY;
    }

    public static int modulesX(Minecraft mc, VisualConfig cfg) {
        int width = VisualHud.activeModulesWidth(mc, cfg);
        int maxX = Math.max(0, mc.getWindow().getGuiScaledWidth() - width);
        return cfg.modulesHudX < 0 ? maxX : clamp(cfg.modulesHudX, 0, maxX);
    }

    public static boolean beginDrag(double mouseX, double mouseY) {
        VisualConfig cfg = VisualConfig.get();
        Minecraft mc = Minecraft.getInstance();
        // HUD elements can be repositioned directly in chat; no separate editor toggle is needed.
        if (!(mc.screen instanceof ChatScreen)) return false;

        if (cfg.activeModulesHud && VisualHud.activeModuleCount(cfg) > 0) {
            int x = modulesX(mc, cfg);
            int y = cfg.modulesHudY;
            int w = VisualHud.activeModulesWidth(mc, cfg);
            int h = VisualHud.activeModulesHeight(cfg);
            if (inside(mouseX, mouseY, x, y, w, h)) {
                dragging = MODULES;
                cfg.modulesHudX = x;
                offsetX = (int) mouseX - x;
                offsetY = (int) mouseY - y;
                return true;
            }
        }
        if (beginStatDrag(mouseX, mouseY, mc, cfg, VisualHud.STAT_FPS, STAT_FPS)) return true;
        if (beginStatDrag(mouseX, mouseY, mc, cfg, VisualHud.STAT_CPS, STAT_CPS)) return true;
        if (beginStatDrag(mouseX, mouseY, mc, cfg, VisualHud.STAT_PING, STAT_PING)) return true;
        if (beginStatDrag(mouseX, mouseY, mc, cfg, VisualHud.STAT_COORDS, STAT_COORDS)) return true;
        if (beginStatDrag(mouseX, mouseY, mc, cfg, VisualHud.STAT_SPEED, STAT_SPEED)) return true;
        if (beginStatDrag(mouseX, mouseY, mc, cfg, VisualHud.STAT_TOTEMS, STAT_TOTEMS)) return true;
        if (beginStatDrag(mouseX, mouseY, mc, cfg, VisualHud.STAT_CLOCK, STAT_CLOCK)) return true;
        if (beginStatDrag(mouseX, mouseY, mc, cfg, VisualHud.STAT_DIRECTION, STAT_DIRECTION)) return true;

        if (cfg.potionHud && inside(mouseX, mouseY, cfg.effectsHudX, cfg.effectsHudY,
            VisualHud.effectsHudWidth(mc, cfg), VisualHud.effectsHudHeight(mc, cfg))) {
            dragging = EFFECTS;
            offsetX = (int) mouseX - cfg.effectsHudX;
            offsetY = (int) mouseY - cfg.effectsHudY;
            return true;
        }
        if (cfg.keystrokes && inside(mouseX, mouseY, keystrokesX(mc, cfg), keystrokesY(mc, cfg), 78, 68)) {
            dragging = KEYS;
            offsetX = (int) mouseX - keystrokesX(mc, cfg);
            offsetY = (int) mouseY - keystrokesY(mc, cfg);
            return true;
        }
        if (cfg.armorHud && inside(mouseX, mouseY, cfg.armorHudX, cfg.armorHudY, 78, 46)) {
            dragging = ARMOR;
            offsetX = (int) mouseX - cfg.armorHudX;
            offsetY = (int) mouseY - cfg.armorHudY;
            return true;
        }
        if (cfg.targetHud && inside(mouseX, mouseY, cfg.targetHudX, cfg.targetHudY, 150, 42)) {
            dragging = TARGET;
            offsetX = (int) mouseX - cfg.targetHudX;
            offsetY = (int) mouseY - cfg.targetHudY;
            return true;
        }
        return false;
    }

    private static boolean beginStatDrag(double mouseX, double mouseY, Minecraft mc,
                                          VisualConfig cfg, int statId, int dragId) {
        if (!VisualHud.isStatEnabled(cfg, statId)) return false;
        int x = VisualHud.statX(cfg, statId);
        int y = VisualHud.statY(cfg, statId);
        int width = VisualHud.statWidth(mc, cfg, statId);
        int height = VisualHud.statHeight();
        if (!inside(mouseX, mouseY, x, y, width, height)) return false;
        dragging = dragId;
        offsetX = (int) mouseX - x;
        offsetY = (int) mouseY - y;
        return true;
    }

    public static void tick(Minecraft mc) {
        if (dragging == -1) return;
        if (!(mc.screen instanceof ChatScreen) || mc.getWindow() == null) {
            endDrag();
            return;
        }

        long handle = mc.getWindow().handle();
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            endDrag();
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer mouseX = stack.mallocDouble(1);
            DoubleBuffer mouseY = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(handle, mouseX, mouseY);
            double guiX = mouseX.get(0) * mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getWidth());
            double guiY = mouseY.get(0) * mc.getWindow().getGuiScaledHeight() / Math.max(1, mc.getWindow().getHeight());
            drag(guiX, guiY, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }

    public static boolean isDragging() {
        return dragging != -1;
    }

    private static void drag(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (dragging == -1) return;
        VisualConfig cfg = VisualConfig.get();
        Minecraft mc = Minecraft.getInstance();
        switch (dragging) {
            case TARGET -> {
                cfg.targetHudX = clamp((int) mouseX - offsetX, 0, Math.max(0, screenWidth - 150));
                cfg.targetHudY = clamp((int) mouseY - offsetY, 0, Math.max(0, screenHeight - 42));
            }
            case ARMOR -> {
                cfg.armorHudX = clamp((int) mouseX - offsetX, 0, Math.max(0, screenWidth - 78));
                cfg.armorHudY = clamp((int) mouseY - offsetY, 0, Math.max(0, screenHeight - 46));
            }
            case KEYS -> {
                cfg.keystrokesHudX = clamp((int) mouseX - offsetX, 0, Math.max(0, screenWidth - 78));
                cfg.keystrokesHudY = clamp((int) mouseY - offsetY, 0, Math.max(0, screenHeight - 68));
            }
            case STAT_FPS, STAT_CPS, STAT_PING, STAT_COORDS, STAT_SPEED,
                 STAT_TOTEMS, STAT_CLOCK, STAT_DIRECTION -> {
                int statId = switch (dragging) {
                    case STAT_FPS -> VisualHud.STAT_FPS;
                    case STAT_CPS -> VisualHud.STAT_CPS;
                    case STAT_PING -> VisualHud.STAT_PING;
                    case STAT_COORDS -> VisualHud.STAT_COORDS;
                    case STAT_SPEED -> VisualHud.STAT_SPEED;
                    case STAT_TOTEMS -> VisualHud.STAT_TOTEMS;
                    case STAT_CLOCK -> VisualHud.STAT_CLOCK;
                    default -> VisualHud.STAT_DIRECTION;
                };
                int width = VisualHud.statWidth(mc, cfg, statId);
                int height = VisualHud.statHeight();
                VisualHud.setStatPosition(cfg, statId,
                    clamp((int) mouseX - offsetX, 0, Math.max(0, screenWidth - width)),
                    clamp((int) mouseY - offsetY, 0, Math.max(0, screenHeight - height)));
            }
            case MODULES -> {
                int width = VisualHud.activeModulesWidth(mc, cfg);
                int height = VisualHud.activeModulesHeight(cfg);
                cfg.modulesHudX = clamp((int) mouseX - offsetX, 0, Math.max(0, screenWidth - width));
                cfg.modulesHudY = clamp((int) mouseY - offsetY, 0, Math.max(0, screenHeight - height));
            }
            case EFFECTS -> {
                int width = VisualHud.effectsHudWidth(mc, cfg);
                int height = VisualHud.effectsHudHeight(mc, cfg);
                cfg.effectsHudX = clamp((int) mouseX - offsetX, 0, Math.max(0, screenWidth - width));
                cfg.effectsHudY = clamp((int) mouseY - offsetY, 0, Math.max(0, screenHeight - height));
            }
            default -> {
            }
        }
    }

    public static void endDrag() {
        if (dragging == -1) return;
        dragging = -1;
        VisualConfig.save();
    }

    public static void drawFrame(GuiGraphics graphics, int x, int y, int width, int height, String label) {
        int color = VisualConfig.accentColor();
        graphics.fill(x - 2, y - 2, x + width + 2, y, color);
        graphics.fill(x - 2, y + height, x + width + 2, y + height + 2, color);
        graphics.fill(x - 2, y, x, y + height, color);
        graphics.fill(x + width, y, x + width + 2, y + height, color);
        graphics.fill(x, y - 13, x + Math.min(width, 58), y - 2, 0xD0203346);
        graphics.pose().pushMatrix();
        float textScale = VisualConfig.moduleTextScale();
        graphics.pose().translate(x + 3.0f, y - 11.0f);
        graphics.pose().scale(textScale, textScale);
        graphics.drawString(Minecraft.getInstance().font, label, 0, 0,
            VisualConfig.moduleTextColor(), false);
        graphics.pose().popMatrix();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
