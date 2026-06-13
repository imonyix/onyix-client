/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 *
 * Velocity - Upgraded with Grim-compatible reduction and anti-cheat bypass.
 * Auto-adjusts velocity reduction based on detected server anti-cheat.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientboundSetEntityMotionPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.anticheat.AntiCheatDetector;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class Velocity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBypass = settings.createGroup("Bypass");

    public final Setting<Boolean> knockback = sgGeneral.add(new BoolSetting.Builder()
        .name("knockback")
        .description("Modifies the amount of knockback you take from attacks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> knockbackHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-horizontal")
        .description("How much horizontal knockback you will take.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(knockback::get)
        .build()
    );

    public final Setting<Double> knockbackVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-vertical")
        .description("How much vertical knockback you will take.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(knockback::get)
        .build()
    );

    public final Setting<Boolean> explosions = sgGeneral.add(new BoolSetting.Builder()
        .name("explosions")
        .description("Modifies your knockback from explosions.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> explosionsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-horizontal")
        .description("How much velocity you will take from explosions horizontally.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(explosions::get)
        .build()
    );

    public final Setting<Double> explosionsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-vertical")
        .description("How much velocity you will take from explosions vertically.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(explosions::get)
        .build()
    );

    public final Setting<Boolean> liquids = sgGeneral.add(new BoolSetting.Builder()
        .name("liquids")
        .description("Modifies the amount you are pushed by flowing liquids.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> liquidsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-horizontal")
        .description("How much velocity you will take from liquids horizontally.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(liquids::get)
        .build()
    );

    public final Setting<Double> liquidsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-vertical")
        .description("How much velocity you will take from liquids vertically.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(liquids::get)
        .build()
    );

    public final Setting<Boolean> entityPush = sgGeneral.add(new BoolSetting.Builder()
        .name("entity-push")
        .description("Modifies the amount you are pushed by entities.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> entityPushAmount = sgGeneral.add(new DoubleSetting.Builder()
        .name("entity-push-amount")
        .description("How much you will be pushed.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(entityPush::get)
        .build()
    );

    public final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
        .name("blocks")
        .description("Prevents you from being pushed out of blocks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> sinking = sgGeneral.add(new BoolSetting.Builder()
        .name("sinking")
        .description("Prevents you from sinking in liquids.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> fishing = sgGeneral.add(new BoolSetting.Builder()
        .name("fishing")
        .description("Prevents you from being pulled by fishing rods.")
        .defaultValue(false)
        .build()
    );

    // Bypass settings
    public final Setting<Boolean> autoBypass = sgBypass.add(new BoolSetting.Builder()
        .name("auto-bypass")
        .description("Automatically adjusts velocity reduction based on detected anti-cheat.")
        .defaultValue(true)
        .build()
    );

    public final Setting<BypassMode> bypassMode = sgBypass.add(new EnumSetting.Builder<BypassMode>()
        .name("bypass-mode")
        .description("How to handle velocity packets for bypass.")
        .defaultValue(BypassMode.Smart)
        .visible(() -> !autoBypass.get() || AntiCheatDetector.INSTANCE.isDetected())
        .build()
    );

    public final Setting<Boolean> grimDecay = sgBypass.add(new BoolSetting.Builder()
        .name("grim-decay")
        .description("Simulates natural velocity decay for GrimAC compliance.")
        .defaultValue(true)
        .visible(() -> autoBypass.get() && AntiCheatDetector.INSTANCE.isGrim())
        .build()
    );

    public final Setting<Boolean> randomizeReduction = sgBypass.add(new BoolSetting.Builder()
        .name("randomize-reduction")
        .description("Adds slight randomization to reduction to avoid pattern detection.")
        .defaultValue(false)
        .build()
    );

    private int tickCounter = 0;
    private Vec3 lastVelocity = Vec3.ZERO;
    private final Random random = new Random();

    public Velocity() {
        super(Categories.Movement, "velocity", "Prevents you from being moved by external forces. Upgraded with anti-cheat bypass.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        tickCounter++;

        if (!sinking.get()) return;
        if (mc.options.keyJump.isDown() || mc.options.keyShift.isDown()) return;

        if ((mc.player.isInWater() || mc.player.isInLava()) && mc.player.getDeltaMovement().y < 0) {
            ((IVec3) mc.player.getDeltaMovement()).meteor$setY(0);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (knockback.get() && event.packet instanceof ClientboundSetEntityMotionPacket packet
            && packet.id() == mc.player.getId()) {

            double hReduction = knockbackHorizontal.get();
            double vReduction = knockbackVertical.get();

            // Auto-bypass adjustment
            if (autoBypass.get() && AntiCheatDetector.INSTANCE.isDetected()) {
                double acReduction = AntiCheatDetector.INSTANCE.getVelocityReduction();

                switch (bypassMode.get()) {
                    case Smart -> {
                        // Grim: reduce but don't fully nullify, simulate natural decay
                        if (AntiCheatDetector.INSTANCE.isGrim()) {
                            hReduction = Math.max(hReduction, acReduction);
                            vReduction = Math.max(vReduction, acReduction);
                        }
                        // Vulcan: can reduce more aggressively
                        else if (AntiCheatDetector.INSTANCE.isVulcan()) {
                            hReduction = Math.max(hReduction, acReduction);
                            vReduction = Math.max(vReduction, acReduction * 0.9);
                        }
                        // Matrix/NCP: moderate reduction
                        else {
                            hReduction = Math.max(hReduction, acReduction);
                            vReduction = Math.max(vReduction, acReduction);
                        }
                    }
                    case Packet -> {
                        // Full cancellation - risky but effective
                        hReduction = 0;
                        vReduction = 0;
                    }
                    case Minimal -> {
                        // Very slight reduction - safest
                        hReduction = Math.max(hReduction, 0.95);
                        vReduction = Math.max(vReduction, 0.95);
                    }
                }
            }

            // Randomize reduction slightly
            if (randomizeReduction.get()) {
                double jitter = (random.nextDouble() - 0.5) * 0.04;
                hReduction = Math.max(0, Math.min(1, hReduction + jitter));
                vReduction = Math.max(0, Math.min(1, vReduction + jitter));
            }

            double velX = (packet.movement().x() - mc.player.getDeltaMovement().x) * hReduction;
            double velY = (packet.movement().y() - mc.player.getDeltaMovement().y) * vReduction;
            double velZ = (packet.movement().z() - mc.player.getDeltaMovement().z) * hReduction;

            Vec3 newVelocity = new Vec3(
                velX + mc.player.getDeltaMovement().x,
                velY + mc.player.getDeltaMovement().y,
                velZ + mc.player.getDeltaMovement().z
            );

            ((ClientboundSetEntityMotionPacketAccessor) (Object) packet).meteor$setMovement(newVelocity);
            lastVelocity = newVelocity;
        }
    }

    public double getHorizontal(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

    public double getVertical(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

    public enum BypassMode {
        Smart,
        Packet,
        Minimal
    }
}
