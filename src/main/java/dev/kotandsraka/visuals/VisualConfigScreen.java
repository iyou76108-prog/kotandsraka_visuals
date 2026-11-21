package dev.kotandsraka.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

public final class VisualConfigScreen extends Screen {
    private static final int BG = 0xFF111319;
    private static final int SIDEBAR = 0xFF181B23;
    private static final int MODULES_BG = 0xFF20232C;
    private static final int SETTINGS_BG = 0xFF242730;
    private static final int TEXT = 0xFFF3F1F5;
    private static final int MUTED = 0xFF9A9DA8;

    private final Screen parent;
    private String category = "Бой";
    private String selectedId = "hitParticles";
    private int moduleRowHeight = 27;
    private boolean draggingColorSquare;
    private boolean draggingHueBar;

    private record Feature(String id, String name, String description, boolean enabled, Runnable toggle) {}

    public VisualConfigScreen(Screen parent) {
        super(Component.literal("KotCode"));
        this.parent = parent;
    }

    private static final String[] CATEGORIES = {
        "Бой", "Движение", "Мир", "Рендер", "Игрок",
        "Другое", "Друзья", "Конфиги"
    };

    private int panelWidth() {
        return Math.max(320, Math.min(930, this.width - 20));
    }

    private int panelHeight() {
        return Math.max(220, Math.min(610, this.height - 16));
    }

    private int left() {
        return (this.width - panelWidth()) / 2;
    }

    private int top() {
        return (this.height - panelHeight()) / 2;
    }

    private int modulesX() {
        return left() + 140;
    }

    private int modulesWidth() {
        return 220;
    }

    private int settingsX() {
        return modulesX() + modulesWidth() + 12;
    }

    private int settingsWidth() {
        return left() + panelWidth() - 14 - settingsX();
    }

    @Override
    protected void init() {
        VisualConfig cfg = VisualConfig.get();
        category = isKnownCategory(cfg.lastMenuCategory) ? cfg.lastMenuCategory : "Бой";
        selectedId = cfg.lastMenuFeature == null ? "" : cfg.lastMenuFeature;

        List<Feature> features = featuresForCategory(cfg);
        if (features.isEmpty()) {
            selectedId = "";
        } else if (features.stream().noneMatch(feature -> feature.id().equals(selectedId))) {
            selectedId = features.get(0).id();
        }
        rememberSelection();
        rebuild();
    }

    private static boolean isKnownCategory(String candidate) {
        if (candidate == null) return false;
        for (String item : CATEGORIES) {
            if (item.equals(candidate)) return true;
        }
        return false;
    }

    private void rememberSelection() {
        VisualConfig cfg = VisualConfig.get();
        cfg.lastMenuCategory = category;
        cfg.lastMenuFeature = selectedId;
        VisualConfig.save();
    }

    private void rebuild() {
        clearWidgets();
        VisualConfig cfg = VisualConfig.get();
        int l = left();
        int t = top();
        int h = panelHeight();

        for (int i = 0; i < CATEGORIES.length; i++) {
            String name = CATEGORIES[i];
            int y = t + 65 + i * 29;
            addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text(name)), b -> {
                category = name;
                List<Feature> next = featuresForCategory(VisualConfig.get());
                selectedId = next.isEmpty() ? "" : next.get(0).id();
                rememberSelection();
                rebuild();
            }).bounds(l + 10, y, 118, 24).build());
        }

        addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.isEnglish() ? "LANGUAGE: EN" : "ЯЗЫК: RU"), b -> {
            cfg.language = KotCodeLanguage.isEnglish() ? "ru" : "en";
            VisualConfig.save();
            rebuild();
        }).bounds(l + panelWidth() - 124, t + 14, 110, 24).build());

        List<Feature> features = featuresForCategory(cfg);
        if (!features.isEmpty() && features.stream().noneMatch(f -> f.id().equals(selectedId))) {
            selectedId = features.get(0).id();
        }

        int listTop = t + 66;
        int listBottom = t + h - 45;
        moduleRowHeight = Math.max(20, Math.min(29,
            (listBottom - listTop) / Math.max(1, features.size())));

        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);
            int y = listTop + i * moduleRowHeight;
            int rowHeight = Math.max(18, moduleRowHeight - 3);
            final String id = feature.id();

            addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text(feature.name())).withStyle(style -> style.withItalic(true)), b -> {
                selectedId = id;
                rememberSelection();
                rebuild();
            }).bounds(modulesX() + 6, y, modulesWidth() - 57, rowHeight).build());

            if (feature.toggle() != null) {
                addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text(feature.enabled() ? "ВКЛ" : "ВЫКЛ")), b -> {
                    feature.toggle().run();
                    VisualConfig.save();
                    rebuild();
                }).bounds(modulesX() + modulesWidth() - 46, y, 39, rowHeight).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("..."), b -> {
                    selectedId = id;
                    rememberSelection();
                    rebuild();
                }).bounds(modulesX() + modulesWidth() - 46, y, 39, rowHeight).build());
            }
        }

        buildSettings(cfg, findSelected(features), l, t, h);

        addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text("СБРОСИТЬ ВСЁ")), b -> {
            String keepCategory = category;
            String keepFeature = selectedId;
            VisualConfig.reset();
            category = keepCategory;
            selectedId = keepFeature;
            List<Feature> resetFeatures = featuresForCategory(VisualConfig.get());
            if (resetFeatures.stream().noneMatch(feature -> feature.id().equals(selectedId))) {
                selectedId = resetFeatures.isEmpty() ? "" : resetFeatures.get(0).id();
            }
            rememberSelection();
            rebuild();
        }).bounds(l + 12, t + h - 34, 112, 24).build());

        addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text("ГОТОВО")), b -> onClose())
            .bounds(l + panelWidth() - 124, t + h - 34, 110, 24).build());
    }

    private Feature findSelected(List<Feature> features) {
        for (Feature feature : features) {
            if (feature.id().equals(selectedId)) return feature;
        }
        return null;
    }

    private List<Feature> featuresForCategory(VisualConfig c) {
        List<Feature> list = new ArrayList<>();
        switch (category) {
            case "Бой" -> {
                add(list, "hitParticles", "Частицы при ударе", "Вспышка частиц при попадании по цели", c.hitParticles, () -> c.hitParticles = !c.hitParticles);
                add(list, "criticalParticles", "Частицы крит. удара", "Дополнительные частицы при критическом ударе", c.criticalParticles, () -> c.criticalParticles = !c.criticalParticles);
                add(list, "hitSound", "Звук попадания", "Воспроизводить звук при попадании по существу", c.hitSound, () -> c.hitSound = !c.hitSound);
                add(list, "hitText", "Метка попадания", "Показывать надпись ПОПАДАНИЕ / КРИТ", c.hitText, () -> c.hitText = !c.hitText);
                add(list, "damageNumbers", "Числа урона", "Показывать нанесённый цели урон", c.damageNumbers, () -> c.damageNumbers = !c.damageNumbers);
                add(list, "killEffect", "Эффект убийства", "Всплеск частиц при смерти цели", c.killEffect, () -> c.killEffect = !c.killEffect);
                add(list, "targetRing", "Кольцо цели", "Кольцевой эффект на атакованной цели", c.targetRing, () -> c.targetRing = !c.targetRing);
            }
            case "Движение" -> {
                add(list, "autoSprint", "Авто-бег", "Автоматически включать бег при движении вперёд", c.autoSprint, () -> c.autoSprint = !c.autoSprint);
                add(list, "playerTrail", "След игрока", "Оставлять след за игроком", c.playerTrail, () -> c.playerTrail = !c.playerTrail);
                add(list, "jumpRings", "Кольцо прыжка", "Кольцо из частиц при прыжке", c.jumpRings, () -> c.jumpRings = !c.jumpRings);
                add(list, "skullJump", "Череп при прыжке", "Показывать череп рядом с прицелом при прыжке", c.skullJump, () -> c.skullJump = !c.skullJump);
            }
            case "Мир" -> {
                add(list, "worldParticles", "Частицы мира", "Окружающие частицы вокруг игрока", c.worldParticles, () -> c.worldParticles = !c.worldParticles);
                add(list, "pearlTrail", "След жемчуга", "След за вашими жемчужинами Края", c.pearlTrail, () -> c.pearlTrail = !c.pearlTrail);
                add(list, "trajectory", "Траектория", "Баллистическая траектория по направлению взгляда", c.trajectory, () -> c.trajectory = !c.trajectory);
                add(list, "arrowTrajectory", "Прогноз стрелы", "Показывать предполагаемый путь выпущенных стрел", c.arrowTrajectory, () -> c.arrowTrajectory = !c.arrowTrajectory);
                add(list, "chinaHat", "Шляпа", "Кольцо над головой в режиме от третьего лица", c.chinaHat, () -> c.chinaHat = !c.chinaHat);
                add(list, "bodyGlow", "Свечение тела", "Декоративные частицы вращаются вокруг вас", c.bodyGlow, () -> c.bodyGlow = !c.bodyGlow);
                add(list, "trailRainbow", "Радужный след", "Плавно менять цвета следа", c.trailRainbow, () -> c.trailRainbow = !c.trailRainbow);
            }
            case "Рендер" -> {
                add(list, "customCrosshair", "Настраиваемый прицел", "Прицел с выбранным цветовым акцентом", c.customCrosshair, () -> c.customCrosshair = !c.customCrosshair);
                add(list, "targetEsp", "Обводка цели", "Кольцо вокруг последней атакованной цели", c.targetEsp, () -> c.targetEsp = !c.targetEsp);
                add(list, "hitRange", "Дальность удара", "Показывать круг радиусом 3 блока", c.hitRange, () -> c.hitRange = !c.hitRange);
                add(list, "hitFlash", "Вспышка при ударе", "Краткая вспышка экрана после попадания", c.hitFlash, () -> c.hitFlash = !c.hitFlash);
                add(list, "playerGlow", "Обводка силуэта игроков", "Контур точно повторяет модель игрока и виден сквозь блоки", c.playerGlow, () -> c.playerGlow = !c.playerGlow);
                add(list, "chestGlow", "Свечение сундуков", "Подсветка сундуков и эндер-сундуков сквозь стены", c.chestGlow, () -> c.chestGlow = !c.chestGlow);
            }
            case "Игрок" -> {
                add(list, "viewModel", "Положение рук", "Изменять положение и поворот рук от первого лица", c.viewModel, () -> c.viewModel = !c.viewModel);
                add(list, "customSwing", "Настройка удара", "Настраивать скорость, силу и углы замаха", c.customSwing, () -> c.customSwing = !c.customSwing);
            }
            case "Другое" -> {
                add(list, "targetHud", "HUD цели", "Имя цели и полоска здоровья", c.targetHud, () -> c.targetHud = !c.targetHud);
                add(list, "armorHud", "HUD брони", "Слоты вашей брони и её прочность", c.armorHud, () -> c.armorHud = !c.armorHud);
                add(list, "enemyArmorHud", "Броня противников", "Показывать броню, прочность и все зачарования над головами игроков", c.enemyArmorHud, () -> c.enemyArmorHud = !c.enemyArmorHud);
                add(list, "keystrokes", "Клавиши", "Отображение нажатий WASD и кнопок мыши", c.keystrokes, () -> c.keystrokes = !c.keystrokes);
                add(list, "fpsHud", "Кадры", "Показывать частоту кадров в секунду", c.fpsHud, () -> c.fpsHud = !c.fpsHud);
                add(list, "cpsHud", "Клики", "Отдельный счётчик кликов ЛКМ и ПКМ", c.cpsHud, () -> c.cpsHud = !c.cpsHud);
                add(list, "pingHud", "Пинг", "Показывать задержку соединения с сервером", c.pingHud, () -> c.pingHud = !c.pingHud);
                add(list, "coordsHud", "Координаты", "Отдельная панель координат X, Y и Z", c.coordsHud, () -> c.coordsHud = !c.coordsHud);
                add(list, "speedHud", "Скорость", "Показывать скорость передвижения в блоках в секунду", c.speedHud, () -> c.speedHud = !c.speedHud);
                add(list, "totemHud", "Тотемы", "Отдельный счётчик тотемов в инвентаре и второй руке", c.totemHud, () -> c.totemHud = !c.totemHud);
                add(list, "potionHud", "Список эффектов", "Названия, уровни и оставшееся время активных эффектов", c.potionHud, () -> c.potionHud = !c.potionHud);
                add(list, "clockHud", "Время", "Отдельно показывать игровое время Minecraft", c.clockHud, () -> c.clockHud = !c.clockHud);
                add(list, "compassHud", "Направление", "Отдельно показывать сторону света, куда смотрит игрок", c.compassHud, () -> c.compassHud = !c.compassHud);
                add(list, "activeModulesHud", "Активные модули", "Список включённых модулей", c.activeModulesHud, () -> c.activeModulesHud = !c.activeModulesHud);
                add(list, "comboHud", "Счётчик комбо", "Показывать количество последовательных попаданий", c.comboHud, () -> c.comboHud = !c.comboHud);
            }
            case "Конфиги" -> {
                list.add(new Feature("theme", "Оформление", "Цветовой акцент и внешний вид меню", false, null));
                list.add(new Feature("hudLayout", "Положение HUD", "Сбросить положение перемещаемых панелей HUD", false, null));
                list.add(new Feature("resetConfig", "Сброс настроек", "Вернуть настройки всех модулей по умолчанию", false, null));
            }
            case "Друзья" -> {
                add(list, "friendGlow", "Обводка друзей",
                    "Подсвечивать игроков, ники которых добавлены в список друзей",
                    c.friendGlow, () -> c.friendGlow = !c.friendGlow);
                list.add(new Feature("friendList", "Список друзей",
                    "Добавляйте ники игроков и настраивайте отдельный цвет их обводки", false, null));
            }
            default -> {
            }
        }
        return list;
    }

    private static void add(List<Feature> list, String id, String name, String description,
                            boolean enabled, Runnable toggle) {
        list.add(new Feature(id, name, description, enabled, toggle));
    }

    private void buildSettings(VisualConfig c, Feature feature, int l, int t, int h) {
        if (feature == null) return;
        int x = settingsX() + 12;
        int colWidth = Math.max(150, settingsWidth() - 24);
        int firstY = t + 132;

        if (feature.toggle() != null) {
            addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text(feature.enabled() ? "ВКЛЮЧЕНО  ●" : "ВЫКЛЮЧЕНО  ○")), b -> {
                feature.toggle().run();
                VisualConfig.save();
                rebuild();
            }).bounds(settingsX() + settingsWidth() - 132, t + 63, 118, 24).build());
        }

        switch (feature.id()) {
            case "hitParticles", "criticalParticles" -> {
                addSlider("КОЛИЧЕСТВО ЧАСТИЦ", x, firstY, colWidth, 2, 24, 2, c.particleCount,
                    v -> c.particleCount = (int) Math.round(v));
                addSetting("ФОРМА: " + particleShapeName(c.particleShape), x, firstY + 34,
                    colWidth, () -> c.particleShape = (c.particleShape + 1) % 3);
                addSlider("РАЗБРОС", x, firstY + 68, colWidth, 0.10, 1.25, 0.05, c.particleSpread,
                    v -> c.particleSpread = (float) v);
                addSlider("СКОРОСТЬ", x, firstY + 102, colWidth, 0.02, 0.50, 0.02, c.particleSpeed,
                    v -> c.particleSpeed = (float) v);
                addSlider("ВЫСОТА", x, firstY + 136, colWidth, 0.05, 1.50, 0.05, c.particleHeight,
                    v -> c.particleHeight = (float) v);
                addSlider("ВРЕМЯ ЖИЗНИ", x, firstY + 170, colWidth, 200, 1200, 50, c.particleLifetimeMs,
                    v -> c.particleLifetimeMs = (int) Math.round(v));
                addSettingToggle("КРИТИЧЕСКИЕ ЧАСТИЦЫ: " + onOff(c.criticalParticles),
                    x, firstY + 204, colWidth, () -> c.criticalParticles = !c.criticalParticles);
            }
            case "playerTrail", "pearlTrail", "trailRainbow" -> {
                addSlider("ШИРИНА СЛЕДА", x, firstY, colWidth, 1.0, 5.0, 0.1, c.trailWidth,
                    v -> c.trailWidth = (float) v);
                addSlider("ДЛИНА СЛЕДА", x, firstY + 34, colWidth, 8, 80, 1, c.trailLength,
                    v -> c.trailLength = (int) Math.round(v));
                addSlider("ПЛОТНОСТЬ СЛЕДА", x, firstY + 68, colWidth, 1, 8, 1, c.trailIntensity,
                    v -> c.trailIntensity = (int) Math.round(v));
                addSettingToggle("РАДУЖНЫЙ ЦВЕТ: " + onOff(c.trailRainbow), x, firstY + 102,
                    colWidth, () -> c.trailRainbow = !c.trailRainbow);
            }
            case "viewModel" -> {
                addSlider("ПОЗИЦИЯ X", x, firstY, colWidth, -0.50, 0.50, 0.01, c.viewModelX,
                    v -> c.viewModelX = (float) v);
                addSlider("ПОЗИЦИЯ Y", x, firstY + 34, colWidth, -0.50, 0.50, 0.01, c.viewModelY,
                    v -> c.viewModelY = (float) v);
                addSlider("ПОЗИЦИЯ Z", x, firstY + 68, colWidth, -0.50, 0.50, 0.01, c.viewModelZ,
                    v -> c.viewModelZ = (float) v);
                addSlider("МАСШТАБ", x, firstY + 102, colWidth, 0.50, 1.50, 0.01, c.viewModelScale,
                    v -> c.viewModelScale = (float) v);
                addSlider("ПОВОРОТ X", x, firstY + 136, colWidth, -180, 180, 1, c.viewModelRotX,
                    v -> c.viewModelRotX = (float) v);
                addSlider("ПОВОРОТ Y", x, firstY + 170, colWidth, -180, 180, 1, c.viewModelRotY,
                    v -> c.viewModelRotY = (float) v);
                addSlider("ПОВОРОТ Z", x, firstY + 204, colWidth, -180, 180, 1, c.viewModelRotZ,
                    v -> c.viewModelRotZ = (float) v);
            }
            case "customSwing" -> {
                addSlider("СКОРОСТЬ ЗАМАХА", x, firstY, colWidth, 0.50, 2.50, 0.01, c.swingSpeed,
                    v -> c.swingSpeed = (float) v);
                addSlider("СИЛА ЗАМАХА", x, firstY + 34, colWidth, 0.25, 1.50, 0.01, c.swingStrength,
                    v -> c.swingStrength = (float) v);
                addSlider("ПОВОРОТ X", x, firstY + 68, colWidth, -90, 90, 1, c.swingRotationX,
                    v -> c.swingRotationX = (float) v);
                addSlider("ПОВОРОТ Y", x, firstY + 102, colWidth, -90, 90, 1, c.swingRotationY,
                    v -> c.swingRotationY = (float) v);
                addSlider("ПОВОРОТ Z", x, firstY + 136, colWidth, -90, 90, 1, c.swingRotationZ,
                    v -> c.swingRotationZ = (float) v);
            }
            case "customCrosshair" -> {
                addSlider("РАЗМЕР", x, firstY + 0, colWidth, 3, 16, 1, c.crosshairSize,
                    v -> c.crosshairSize = (int) Math.round(v));
                addSlider("ЗАЗОР", x, firstY + 34, colWidth, 1, 12, 1, c.crosshairGap,
                    v -> c.crosshairGap = (int) Math.round(v));
                addSlider("ТОЛЩИНА", x, firstY + 68, colWidth, 1, 6, 1, c.crosshairThickness,
                    v -> c.crosshairThickness = (int) Math.round(v));
            }
            case "theme" -> {
                addSlider("ПРОЗРАЧНОСТЬ HUD", x, firstY + 205, colWidth, 0.25, 1.0, 0.01,
                    c.hudOpacity, v -> c.hudOpacity = (float) v);
                addSettingToggle("СТЕКЛЯННОЕ МЕНЮ: " + onOff(c.glassMenu),
                    x, firstY + 239, colWidth, () -> c.glassMenu = !c.glassMenu);
            }
            case "hudLayout" -> {
                addSetting("СБРОСИТЬ ПОЗИЦИИ HUD", x, firstY, colWidth, VisualConfig::resetHudPositions);
                addSlider("ПРОЗРАЧНОСТЬ HUD", x, firstY + 34, colWidth, 0.25, 1.0, 0.01,
                    c.hudOpacity, v -> c.hudOpacity = (float) v);
            }
            case "resetConfig" -> addSetting("СБРОСИТЬ НАСТРОЙКИ", x, firstY,
                colWidth, VisualConfig::reset);
            case "friendList" -> {
                int fieldWidth = Math.max(70, colWidth - 66);
                EditBox nameField = new EditBox(this.font, x, firstY, fieldWidth, 24, Component.empty());
                nameField.setMaxLength(16);
                addRenderableWidget(nameField);
                addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text("Добавить")), b -> {
                    String name = nameField.getValue().trim();
                    if (name.matches("[A-Za-z0-9_]{1,16}") && c.friends != null
                        && c.friends.stream().noneMatch(existing -> existing.equalsIgnoreCase(name))) {
                        c.friends.add(name);
                        VisualConfig.save();
                        rebuild();
                    }
                }).bounds(x + fieldWidth + 6, firstY, Math.max(58, colWidth - fieldWidth - 6), 24).build());

                if (c.friends != null) {
                    int visibleFriends = Math.min(6, c.friends.size());
                    for (int i = 0; i < visibleFriends; i++) {
                        String friendName = c.friends.get(i);
                        int rowY = firstY + 31 + i * 24;
                        addRenderableWidget(Button.builder(Component.literal(friendName + "  ×"), b -> {
                            c.friends.removeIf(existing -> existing.equalsIgnoreCase(friendName));
                            VisualConfig.save();
                            rebuild();
                        }).bounds(x, rowY, colWidth, 21).build());
                    }
                }
            }
            default -> {
                // Features without numeric options keep their controls simple and safe.
            }
        }
    }

    private void addSlider(String label, int x, int y, int width,
                            double min, double max, double step, double initial,
                            DoubleConsumer setter) {
        addRenderableWidget(new ValueSlider(x, y, width, label, min, max, step, initial, setter));
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleConsumer setter;

        private ValueSlider(int x, int y, int width, String label,
                            double min, double max, double step, double initial,
                            DoubleConsumer setter) {
            super(x, y, width, 28, Component.literal(label), normalize(initial, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.setter = setter;
            updateMessage();
        }

        private static double normalize(double value, double min, double max) {
            if (max <= min) return 0.0;
            return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
        }

        private double actualValue() {
            double raw = min + value * (max - min);
            double snapped = min + Math.round((raw - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        @Override
        protected void updateMessage() {
            double current = actualValue();
            String formatted = step >= 1.0 ? String.format("%.0f", current)
                : step >= 0.1 ? String.format("%.1f", current) : String.format("%.2f", current);
            setMessage(Component.literal(KotCodeLanguage.text(label) + "   " + formatted));
        }

        @Override
        protected void applyValue() {
            setter.accept(actualValue());
        }

        @Override
        public void onRelease(MouseButtonEvent click) {
            super.onRelease(click);
            VisualConfig.save();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            graphics.fill(x, y, x + width, y + getHeight(), 0xFF191D26);
            graphics.drawString(Minecraft.getInstance().font, getMessage(), x + 4, y + 3,
                0xFFE0E2E8, false);

            int trackLeft = x + 5;
            int trackRight = x + width - 5;
            int trackY = y + 20;
            int trackWidth = Math.max(1, trackRight - trackLeft);
            int filled = (int) Math.round(trackWidth * value);
            graphics.fill(trackLeft, trackY, trackRight, trackY + 3, 0xFF414652);
            graphics.fill(trackLeft, trackY, trackLeft + filled, trackY + 3, VisualConfig.accentColor());
            int knobX = trackLeft + filled;
            graphics.fill(knobX - 3, trackY - 4, knobX + 4, trackY + 7, 0xFFF7F4F8);
            graphics.fill(knobX - 2, trackY - 3, knobX + 3, trackY + 6, VisualConfig.accentColor());
        }
    }

    private int pickerX() {
        return settingsX() + 14;
    }

    private int pickerY() {
        return top() + ("friendList".equals(selectedId) ? 340 : 148);
    }

    private boolean hasColorPicker() {
        return "theme".equals(selectedId) || "friendList".equals(selectedId);
    }

    private int pickerWidth() {
        return Math.max(180, Math.min(320, settingsWidth() - 28));
    }

    private int pickerHeight() {
        return 118;
    }

    private int hueBarY() {
        return pickerY() + pickerHeight() + 12;
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private void updateColorPicker(double mouseX, double mouseY, boolean hue) {
        VisualConfig cfg = VisualConfig.get();
        boolean friendPicker = "friendList".equals(selectedId);
        if (hue) {
            float amount = (float) ((mouseX - pickerX()) / Math.max(1.0, pickerWidth()));
            if (friendPicker) cfg.friendColorHue = Math.max(0.0f, Math.min(359.9f, amount * 360.0f));
            else cfg.colorHue = Math.max(0.0f, Math.min(359.9f, amount * 360.0f));
        } else {
            float saturation = (float) ((mouseX - pickerX()) / Math.max(1.0, pickerWidth()));
            float value = 1.0f - (float) ((mouseY - pickerY()) / Math.max(1.0, pickerHeight()));
            if (friendPicker) {
                cfg.friendColorSaturation = Math.max(0.0f, Math.min(1.0f, saturation));
                cfg.friendColorValue = Math.max(0.0f, Math.min(1.0f, value));
            } else {
                cfg.colorSaturation = Math.max(0.0f, Math.min(1.0f, saturation));
                cfg.colorValue = Math.max(0.0f, Math.min(1.0f, value));
            }
        }
    }

    private void drawColorPicker(GuiGraphics graphics) {
        if (!hasColorPicker()) return;

        VisualConfig cfg = VisualConfig.get();
        boolean friendPicker = "friendList".equals(selectedId);
        float hue = friendPicker ? cfg.friendColorHue : cfg.colorHue;
        float saturationValue = friendPicker ? cfg.friendColorSaturation : cfg.colorSaturation;
        float brightnessValue = friendPicker ? cfg.friendColorValue : cfg.colorValue;
        int color = friendPicker ? VisualConfig.friendAccentColor() : VisualConfig.accentColor();
        int x = pickerX();
        int y = pickerY();
        int width = pickerWidth();
        int height = pickerHeight();

        graphics.drawString(this.font,
            KotCodeLanguage.text(friendPicker ? "ЦВЕТ ОБВОДКИ ДРУЗЕЙ" : "ВЫБОР ЦВЕТА"),
            x, y - 18, color, true);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF080A0F);

        int columns = 64;
        int rows = 24;
        for (int column = 0; column < columns; column++) {
            int x1 = x + column * width / columns;
            int x2 = x + (column + 1) * width / columns;
            float saturation = (column + 0.5f) / columns;
            for (int row = 0; row < rows; row++) {
                int y1 = y + row * height / rows;
                int y2 = y + (row + 1) * height / rows;
                float value = 1.0f - (row + 0.5f) / rows;
                graphics.fill(x1, y1, x2, y2, VisualConfig.hsvToRgb(hue, saturation, value));
            }
        }

        int cursorX = x + Math.round(saturationValue * width);
        int cursorY = y + Math.round((1.0f - brightnessValue) * height);
        graphics.fill(cursorX - 4, cursorY - 4, cursorX + 5, cursorY + 5, 0xFF101116);
        graphics.fill(cursorX - 2, cursorY - 2, cursorX + 3, cursorY + 3, 0xFFFFFFFF);

        int hueY = hueBarY();
        int segments = 72;
        for (int i = 0; i < segments; i++) {
            int x1 = x + i * width / segments;
            int x2 = x + (i + 1) * width / segments;
            graphics.fill(x1, hueY, x2, hueY + 10,
                VisualConfig.hsvToRgb(i * 360.0f / segments, 1.0f, 1.0f));
        }
        int hueX = x + Math.round(hue / 360.0f * width);
        graphics.fill(hueX - 3, hueY - 2, hueX + 4, hueY + 12, 0xFFFFFFFF);
        graphics.fill(hueX - 1, hueY, hueX + 2, hueY + 10,
            VisualConfig.hsvToRgb(hue, 1.0f, 1.0f));

        String hex = String.format("#%02X%02X%02X", (color >> 16) & 255, (color >> 8) & 255, color & 255);
        String rgb = String.format("RGB  %d, %d, %d", (color >> 16) & 255, (color >> 8) & 255, color & 255);
        int textY = hueY + 18;
        graphics.fill(x, textY - 3, x + Math.min(width, 108), textY + 15, 0xFF12151C);
        graphics.drawString(this.font, hex, x + 7, textY, 0xFFF4F2F6, false);
        graphics.drawString(this.font, rgb, x + 120, textY, 0xFFB7BBC5, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (GLFW.GLFW_MOUSE_BUTTON_LEFT == click.button() && hasColorPicker()) {
            if (inside(click.x(), click.y(), pickerX(), pickerY(), pickerWidth(), pickerHeight())) {
                draggingColorSquare = true;
                updateColorPicker(click.x(), click.y(), false);
                return true;
            }
            if (inside(click.x(), click.y(), pickerX(), hueBarY(), pickerWidth(), 10)) {
                draggingHueBar = true;
                updateColorPicker(click.x(), click.y(), true);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (draggingColorSquare || draggingHueBar) {
            updateColorPicker(click.x(), click.y(), draggingHueBar);
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (draggingColorSquare || draggingHueBar) {
            draggingColorSquare = false;
            draggingHueBar = false;
            VisualConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    private void addSetting(String label, int x, int y, int width, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text(label)), b -> {
            action.run();
            VisualConfig.save();
            rebuild();
        }).bounds(x, y, width, 25).build());
    }

    private void addSettingToggle(String label, int x, int y, int width, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(KotCodeLanguage.text(label)), b -> {
            action.run();
            VisualConfig.save();
            rebuild();
        }).bounds(x, y, width, 25).build());
    }

    private static String onOff(boolean enabled) {
        return KotCodeLanguage.text(enabled ? "ВКЛ" : "ВЫКЛ");
    }

    private static String fmt(float value) {
        return String.format("%.2f", value);
    }

    private static float stepFloat(float value, float min, float max, float step) {
        float next = value + step;
        return next > max + 0.0001f ? min : Math.round(next * 100.0f) / 100.0f;
    }

    private static String particleShapeName(int shape) {
        return switch (shape) {
            case 1 -> KotCodeLanguage.text("КОЛЬЦО");
            case 2 -> KotCodeLanguage.text("ВВЕРХ");
            default -> KotCodeLanguage.text("ВСПЫШКА");
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int l = left();
        int t = top();
        int w = panelWidth();
        int h = panelHeight();
        int sx = settingsX();
        int sw = settingsWidth();

        g.fill(0, 0, this.width, this.height, 0xB800050A);
        g.fill(l, t, l + w, t + h, BG);
        g.fill(l, t, l + 130, t + h, SIDEBAR);
        g.fill(l + 136, t + 52, l + 136 + modulesWidth() + 2, t + h - 43, MODULES_BG);
        g.fill(sx, t + 52, sx + sw, t + h - 43, SETTINGS_BG);

        g.fill(l, t, l + w, t + 3, VisualConfig.accentColor());
        g.fill(l, t, l + 3, t + h, VisualConfig.accentColor());
        g.fill(sx, t + 52, sx + sw, t + 53, VisualConfig.accentColor() & 0x99FFFFFF);
        g.fill(l + 12, t + h - 40, l + w - 12, t + h - 39, 0x554D5360);

        g.drawString(this.font, "KotCode", l + 18, t + 17, VisualConfig.accentColor(), true);
        g.drawString(this.font, KotCodeLanguage.text("KOTCODE  /  ВИЗУАЛЬНЫЙ КЛИЕНТ"), l + 148, t + 16, TEXT, true);
        g.drawString(this.font, KotCodeLanguage.text("НАСТРОЙКИ КЛИЕНТА"), l + 148, t + 31, MUTED, false);
        g.drawString(this.font, KotCodeLanguage.text(category), modulesX() + 7, t + 59, TEXT, true);

        List<Feature> features = featuresForCategory(VisualConfig.get());
        if (features.isEmpty()) {
            g.drawString(this.font, KotCodeLanguage.text("В этой категории пока нет модулей."), modulesX() + 12, t + 83, MUTED, false);
        } else {
            int listTop = t + 66;
            for (int i = 0; i < features.size(); i++) {
                Feature feature = features.get(i);
                int y = listTop + i * moduleRowHeight;
                if (feature.id().equals(selectedId)) {
                    g.fill(modulesX() + 3, y - 1, modulesX() + modulesWidth() - 3,
                        y + moduleRowHeight - 2, 0xAA4A2435);
                    g.fill(modulesX() + 3, y - 1, modulesX() + 6,
                        y + moduleRowHeight - 2, VisualConfig.accentColor());
                }
            }
        }

        Feature selected = findSelected(features);
        if (selected != null) {
            g.drawString(this.font, KotCodeLanguage.text(category).toUpperCase() + "  /  " + KotCodeLanguage.text(selected.name()),
                sx + 12, t + 62, TEXT, true);
            g.drawString(this.font, KotCodeLanguage.text(selected.description()), sx + 12, t + 83, MUTED, false);
            g.fill(sx + 12, t + 99, sx + sw - 12, t + 100, 0x664D5360);
            g.drawString(this.font, KotCodeLanguage.text("НАСТРОЙКИ"), sx + 12, t + 111, VisualConfig.accentColor(), true);
            if (selected.id().equals("friendList")) {
                List<String> friends = VisualConfig.get().friends;
                if (friends == null || friends.isEmpty()) {
                    g.drawString(this.font, KotCodeLanguage.text("Пока нет добавленных друзей."),
                        sx + 12, t + 181, MUTED, false);
                } else if (friends.size() > 6) {
                    g.drawString(this.font, KotCodeLanguage.text("Показаны первые 6 друзей. Удаляйте их нажатием на ник."),
                        sx + 12, t + 318, MUTED, false);
                }
            } else if (selected.toggle() == null) {
                g.drawString(this.font, KotCodeLanguage.text("Выберите параметр ниже."), sx + 12, t + 128, MUTED, false);
            } else if (selected.id().equals("targetEsp")) {
                g.drawString(this.font, KotCodeLanguage.text("Рисует кольцо вокруг последней атакованной цели."), sx + 12, t + 133, MUTED, false);
            } else if (selected.id().equals("trajectory") || selected.id().equals("arrowTrajectory")) {
                g.drawString(this.font, KotCodeLanguage.text("Прогноз отображается в игровом мире."), sx + 12, t + 133, MUTED, false);
            } else if (selected.id().equals("hitRange")) {
                g.drawString(this.font, KotCodeLanguage.text("Радиус кольца фиксирован: 3 блока."), sx + 12, t + 133, MUTED, false);
            } else if (selected.id().equals("resetConfig")) {
                g.drawString(this.font, KotCodeLanguage.text("Вернуть переключатели и параметры по умолчанию."), sx + 12, t + 133, MUTED, false);
            }
        }

        g.drawString(this.font, "KOTCODE  •  1.0.13", l + 18, t + h - 55, MUTED, false);
        g.drawString(this.font, KotCodeLanguage.text("RIGHT SHIFT  •  ВЫБЕРИ МОДУЛЬ  •  ИЗМЕНИ ПАРАМЕТРЫ"),
            l + 148, t + h - 28, MUTED, false);

        super.render(g, mouseX, mouseY, delta);
        drawColorPicker(g);

        // Accent edges keep the controls consistent with the selected color.
        for (var child : this.children()) {
            if (child instanceof Button button && button.visible) {
                int bx = button.getX();
                int by = button.getY();
                int bw = button.getWidth();
                int bh = button.getHeight();
                g.fill(bx, by, bx + bw, by + 1, (VisualConfig.accentColor() & 0xAAFFFFFF));
                g.fill(bx, by + bh - 1, bx + bw, by + bh, (VisualConfig.accentColor() & 0xAAFFFFFF));
            }
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
