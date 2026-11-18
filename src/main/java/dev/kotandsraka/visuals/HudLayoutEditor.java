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
    private static final int INFO = 3;
    private static final int MODULES = 4;

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
        return cfg.modulesHudX < 0 ? Math.max(0, mc.getWindow().getGuiScaledWidth() - 112) : cfg.modulesHudX;
    }

    public static boolean beginDrag(double mouseX, double mouseY) {
        VisualConfig cfg = VisualConfig.get();
        if (!cfg.hudEditMode) return false;
        Minecraft mc = Minecraft.getInstance();

        if (cfg.activeModulesHud && VisualHud.activeModuleCount(cfg) > 0) {
            int x = modulesX(mc, cfg);
            int y = cfg.modulesHudY;
            int h = VisualHud.activeModuleCount(cfg) * 11 + 22;
            if (inside(mouseX, mouseY, x, y, 150, h)) {
                dragging = MODULES;
                cfg.modulesHudX = x;
                offsetX = (int) mouseX - x;
                offsetY = (int) mouseY - y;
                return true;
            }
        }
        int infoRows = VisualHud.infoHudLineCount(cfg);
        if (infoRows > 0 && inside(mouseX, mouseY, cfg.hudX - 5, cfg.hudY - 5, 225, infoRows * 12 + 12)) {
            dragging = INFO;
            offsetX = (int) mouseX - (cfg.hudX - 5);
            offsetY = (int) mouseY - (cfg.hudY - 5);
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

    public static void tick(Minecraft mc) {
        if (dragging == -1) return;
        if (!(mc.screen instanceof ChatScreen) || mc.getWindow() == null) {
            endDrag();
            return;
        }

        long handle = mc.getWindow().handle();
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) {
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
            case INFO -> {
                cfg.hudX = clamp((int) mouseX - offsetX + 5, 0, Math.max(0, screenWidth - 225));
                cfg.hudY = clamp((int) mouseY - offsetY + 5, 0, Math.max(0, screenHeight - 160));
            }
            case MODULES -> {
                cfg.modulesHudX = clamp((int) mouseX - offsetX, 0, Math.max(0, screenWidth - 150));
                cfg.modulesHudY = clamp((int) mouseY - offsetY, 0, Math.max(0, screenHeight - 200));
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
        int color = 0xFF61DFFF;
        graphics.fill(x - 2, y - 2, x + width + 2, y, color);
        graphics.fill(x - 2, y + height, x + width + 2, y + height + 2, color);
        graphics.fill(x - 2, y, x, y + height, color);
        graphics.fill(x + width, y, x + width + 2, y + height, color);
        graphics.fill(x, y - 13, x + Math.min(width, 58), y - 2, 0xD0203346);
        graphics.drawString(Minecraft.getInstance().font, label, x + 3, y - 11, 0xFFFFFFFF, false);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
