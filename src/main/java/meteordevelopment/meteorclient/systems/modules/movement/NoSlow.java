/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 *
 * NoSlow - Upgraded with Matrix/Grim patches and anti-cheat bypass.
 * Auto-adjusts speed retention based on detected server anti-cheat.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.anticheat.AntiCheatDetector;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

public class NoSlow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBypass = settings.createGroup("Bypass");

    private final Setting<Boolean> items = sgGeneral.add(new BoolSetting.Builder()
        .name("items")
        .description("Whether or not using items will slow you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<WebMode> web = sgGeneral.add(new EnumSetting.Builder<WebMode>()
        .name("web")
        .description("Whether or not cobwebs will not slow you down.")
        .defaultValue(WebMode.Vanilla)
        .build()
    );

    private final Setting<Double> webTimer = sgGeneral.add(new DoubleSetting.Builder()
        .name("web-timer")
        .description("The timer value for WebMode Timer.")
        .defaultValue(10)
        .min(1)
        .sliderMin(1)
        .visible(() -> web.get() == WebMode.Timer)
        .build()
    );

    private final Setting<Boolean> honeyBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("honey-block")
        .description("Whether or not honey blocks will not slow you down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> soulSand = sgGeneral.add(new BoolSetting.Builder()
        .name("soul-sand")
        .description("Whether or not soul sand will not slow you down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> slimeBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("slime-block")
        .description("Whether or not slime blocks will not slow you down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> berryBush = sgGeneral.add(new BoolSetting.Builder()
        .name("berry-bush")
        .description("Whether or not berry bushes will not slow you down.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> airStrict = sgGeneral.add(new BoolSetting.Builder()
        .name("air-strict")
        .description("Will attempt to bypass anti-cheats like 2b2t's.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fluidDrag = sgGeneral.add(new BoolSetting.Builder()
        .name("fluid-drag")
        .description("Whether or not fluid drag will not slow you down.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("sneaking")
        .description("Whether or not sneaking will not slow you down.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> hunger = sgGeneral.add(new BoolSetting.Builder()
        .name("hunger")
        .description("Whether or not hunger will not slow you down.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> slowness = sgGeneral.add(new BoolSetting.Builder()
        .name("slowness")
        .description("Whether or not slowness will not slow you down.")
        .defaultValue(false)
        .build()
    );

    // Bypass settings
    private final Setting<Boolean> autoBypass = sgBypass.add(new BoolSetting.Builder()
        .name("auto-bypass")
        .description("Automatically adjusts NoSlow behavior based on detected anti-cheat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<BypassMode> bypassMode = sgBypass.add(new EnumSetting.Builder<BypassMode>()
        .name("bypass-mode")
        .description("Method to bypass anti-cheat slowdown checks.")
        .defaultValue(BypassMode.Smart)
        .visible(() -> autoBypass.get() && AntiCheatDetector.INSTANCE.isDetected())
        .build()
    );

    private final Setting<Double> grimSpeedFactor = sgBypass.add(new DoubleSetting.Builder()
        .name("grim-speed-factor")
        .description("Speed retention factor for GrimAC (1.0 = full speed, 0.0 = no bypass).")
        .defaultValue(0.8)
        .min(0)
        .sliderMax(1)
        .visible(() -> autoBypass.get() && AntiCheatDetector.INSTANCE.isGrim())
        .build()
    );

    private final Setting<Double> matrixSpeedFactor = sgBypass.add(new DoubleSetting.Builder()
        .name("matrix-speed-factor")
        .description("Speed retention factor for Matrix.")
        .defaultValue(0.75)
        .min(0)
        .sliderMax(1)
        .visible(() -> autoBypass.get() && AntiCheatDetector.INSTANCE.isMatrix())
        .build()
    );

    private final Setting<Boolean> groundSpoof = sgBypass.add(new BoolSetting.Builder()
        .name("ground-spoof")
        .description("Spoofs ground state while using items to bypass some ACs.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> releasePacket = sgBypass.add(new BoolSetting.Builder()
        .name("release-packet")
        .description("Sends a release use packet before movement to bypass Grim item slowdown.")
        .defaultValue(true)
        .visible(() -> autoBypass.get() && AntiCheatDetector.INSTANCE.isGrim())
        .build()
    );

    private boolean resetTimer;
    private final Random random = new Random();

    public NoSlow() {
        super(Categories.Movement, "no-slow", "Allows you to move normally when using objects that slow you. Upgraded with anti-cheat bypass.");
    }

    @Override
    public void onActivate() {
        resetTimer = false;
    }

    public boolean airStrict() {
        return isActive() && airStrict.get() && mc.player.isUsingItem();
    }

    public boolean items() {
        return isActive() && items.get();
    }

    public boolean honeyBlock() {
        return isActive() && honeyBlock.get();
    }

    public boolean soulSand() {
        return isActive() && soulSand.get();
    }

    public boolean slimeBlock() {
        return isActive() && slimeBlock.get();
    }

    public boolean cobweb() {
        return isActive() && web.get() == WebMode.Vanilla;
    }

    public boolean berryBush() {
        return isActive() && berryBush.get();
    }

    public boolean fluidDrag() {
        return isActive() && fluidDrag.get();
    }

    public boolean sneaking() {
        return isActive() && sneaking.get();
    }

    public boolean hunger() {
        return isActive() && hunger.get();
    }

    public boolean slowness() {
        return isActive() && slowness.get();
    }

    /**
     * Get the effective speed factor for NoSlow, accounting for anti-cheat bypass.
     */
    public double getSpeedFactor() {
        if (!isActive()) return 0.2; // Vanilla slowdown factor

        if (autoBypass.get() && AntiCheatDetector.INSTANCE.isDetected()) {
            return AntiCheatDetector.INSTANCE.getNoSlowFactor();
        }

        return 1.0; // Full speed, no bypass needed
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (web.get() == WebMode.Timer) {
            if (mc.level.getBlockState(mc.player.blockPosition()).getBlock() == Blocks.COBWEB && !mc.player.onGround()) {
                resetTimer = false;
                Modules.get().get(Timer.class).setOverride(webTimer.get());
            } else if (!resetTimer) {
                Modules.get().get(Timer.class).setOverride(Timer.OFF);
                resetTimer = true;
            }
        }

        // Ground spoofing for anti-cheat bypass
        if (groundSpoof.get() && mc.player.isUsingItem()) {
            mc.player.setOnGround(true);
        }

        // Auto-bypass: Grim specific handling
        if (autoBypass.get() && AntiCheatDetector.INSTANCE.isGrim() && mc.player.isUsingItem()) {
            if (bypassMode.get() == BypassMode.Smart) {
                // Grim checks for item use + movement combination
                // Slow down slightly but don't fully slow
                double factor = grimSpeedFactor.get();
                // Apply factor via velocity modification
                if (mc.player.getDeltaMovement().horizontalDistance() > 0.2 * factor) {
                    var delta = mc.player.getDeltaMovement();
                    double scale = (0.2 * factor) / delta.horizontalDistance();
                    mc.player.setDeltaMovement(
                        delta.x * scale,
                        delta.y,
                        delta.z * scale
                    );
                }
            }
        }

        // Auto-bypass: Matrix specific handling
        if (autoBypass.get() && AntiCheatDetector.INSTANCE.isMatrix() && mc.player.isUsingItem()) {
            if (bypassMode.get() == BypassMode.Smart) {
                double factor = matrixSpeedFactor.get();
                var delta = mc.player.getDeltaMovement();
                double maxSpeed = 0.2 * factor;
                if (delta.horizontalDistance() > maxSpeed) {
                    double scale = maxSpeed / delta.horizontalDistance();
                    mc.player.setDeltaMovement(
                        delta.x * scale,
                        delta.y,
                        delta.z * scale
                    );
                }
            }
        }

        // Vulcan/NCP: less strict on NoSlow, just use standard bypass
        if (autoBypass.get() && (AntiCheatDetector.INSTANCE.isVulcan() || AntiCheatDetector.INSTANCE.isNCP())) {
            if (mc.player.isUsingItem() && mc.player.getDeltaMovement().horizontalDistance() > 0.22) {
                var delta = mc.player.getDeltaMovement();
                double scale = 0.22 / delta.horizontalDistance();
                mc.player.setDeltaMovement(
                    delta.x * scale,
                    delta.y,
                    delta.z * scale
                );
            }
        }
    }

    public enum WebMode {
        Vanilla,
        Timer,
        None
    }

    public enum BypassMode {
        Smart,
        Full,
        Minimal
    }
}
