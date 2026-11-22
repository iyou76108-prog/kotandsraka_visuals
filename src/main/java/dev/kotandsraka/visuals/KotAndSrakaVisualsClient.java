package dev.kotandsraka.visuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import org.lwjgl.glfw.GLFW;

public final class KotAndSrakaVisualsClient implements ClientModInitializer {
    public static KeyMapping OPEN_MENU;
    private static long nextAutoClickNanos;
    private static long nextAutoBlockPlaceNanos;
    private static boolean wasOnGround = true;
    private static boolean rightShiftWasDown = false;

    @Override
    public void onInitializeClient() {
        KotCodeSounds.register();
        VisualConfig.load();
        WorldTrailRenderer.register();
        XrayRenderer.register();
        OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.kotandsraka_visuals.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("kotandsraka_visuals", "main"))
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HudLayoutEditor.tick(client);
            boolean rightShiftDown = client.getWindow() != null
                && GLFW.glfwGetKey(client.getWindow().handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            boolean rightShiftPressed = rightShiftDown && !rightShiftWasDown;
            rightShiftWasDown = rightShiftDown;
            if ((OPEN_MENU.consumeClick() || rightShiftPressed) && client.screen == null) {
                client.setScreen(new VisualConfigScreen(null));
            }
            if (client.player == null || client.level == null) return;

            VisualConfig cfg = VisualConfig.get();
            tickAutoClicker(client, cfg);
            tickAutoBlockPlacement(client, cfg);
            try {
                VisualHud.tick();
                WorldTrailRenderer.tick();
                VisualEffects.worldAmbience();
            } catch (Throwable visualError) {
                // Visuals must never be able to kick the client out of a world.
                System.err.println("[KotAndSraka Visuals] Disabled one visual tick after an error: " + visualError);
            }

            if (cfg.autoJump && client.screen == null && client.options.keyJump.isDown()
                && client.player.isAlive() && client.player.onGround() && !client.player.isPassenger()
                && !client.player.isFallFlying()) {
                client.player.jumpFromGround();
            }

            boolean ground = client.player.onGround();
            if (cfg.autoSprint && client.player.isAlive() && ground
                && client.player.zza > 0.0F && !client.player.isSprinting()) {
                client.player.setSprinting(true);
            }

            if (cfg.playerTrail && client.player.getDeltaMovement().horizontalDistanceSqr() > 0.0025) {
                for (int i = 0; i < cfg.trailIntensity; i++) {
                    double ox = (client.level.random.nextDouble() - .5) * .35;
                    double oy = client.level.random.nextDouble() * .9;
                    double oz = (client.level.random.nextDouble() - .5) * .35;
                    client.level.addParticle(ParticleTypes.END_ROD,
                        client.player.getX() + ox, client.player.getY() + oy, client.player.getZ() + oz,
                        0, .01, 0);
                }
            }

            if (!ground && wasOnGround) {
                VisualHud.onJump();
                if (cfg.jumpRings) WorldTrailRenderer.spawnJumpRing(client.player);
            }
            wasOnGround = ground;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && entity instanceof LivingEntity living) {
                HitParticles.spawn(living);
                VisualHud.onHit(living);
            }
            return InteractionResult.PASS;
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("kotandsraka_visuals", "hud"), VisualHud::render);
    }

    private static void tickAutoBlockPlacement(net.minecraft.client.Minecraft client, VisualConfig cfg) {
        boolean active = cfg.autoBlockPlacement && client.screen == null
            && client.player != null && client.player.isAlive()
            && !client.player.isSpectator() && client.options.keyUse.isDown()
            && client.gameMode != null && client.hitResult instanceof BlockHitResult;

        if (!active) {
            nextAutoBlockPlaceNanos = 0L;
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) client.hitResult;
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            nextAutoBlockPlaceNanos = 0L;
            return;
        }

        InteractionHand hand;
        if (client.player.getMainHandItem().getItem() instanceof BlockItem) {
            hand = InteractionHand.MAIN_HAND;
        } else if (client.player.getOffhandItem().getItem() instanceof BlockItem) {
            hand = InteractionHand.OFF_HAND;
        } else {
            nextAutoBlockPlaceNanos = 0L;
            return;
        }

        int blocksPerSecond = Math.max(1, Math.min(20, Math.round(cfg.autoBlockPlacementSpeed)));
        long intervalNanos = 1_000_000_000L / blocksPerSecond;
        long now = System.nanoTime();
        if (nextAutoBlockPlaceNanos == 0L || now >= nextAutoBlockPlaceNanos) {
            // Vanilla's interaction manager performs the normal client prediction
            // and sends the regular block-use action to the server.
            client.gameMode.useItemOn(client.player, hand, blockHit);
            nextAutoBlockPlaceNanos = now + intervalNanos;
        }
    }

    private static void tickAutoClicker(net.minecraft.client.Minecraft client, VisualConfig cfg) {
        boolean active = cfg.autoClicker && client.screen == null
            && client.player != null && client.player.isAlive()
            && !client.player.isSpectator() && client.options.keyAttack.isDown();

        if (!active) {
            nextAutoClickNanos = 0L;
            return;
        }

        int cps = Math.max(1, Math.min(20, Math.round(cfg.autoClickerCps)));
        long intervalNanos = 1_000_000_000L / cps;
        long now = System.nanoTime();
        if (nextAutoClickNanos == 0L || now >= nextAutoClickNanos) {
            ((dev.kotandsraka.visuals.mixin.MinecraftAttackInvoker) (Object) client).kotcode$startAttack();
            nextAutoClickNanos = now + intervalNanos;
        }
    }
}
