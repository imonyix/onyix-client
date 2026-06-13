/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 *
 * Flight - Upgraded with anti-cheat bypass for GrimAC, Vulcan, Matrix, Spartan, NCP.
 * Auto-adjusts speed and movement patterns based on detected server anti-cheat.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.LocalPlayerAccessor;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.anticheat.AntiCheatDetector;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class Flight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBypass = settings.createGroup("Bypass");
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode for Flight.")
        .defaultValue(Mode.Abilities)
        .onChanged(mode -> {
            if (!isActive() || !Utils.canUpdate()) return;
            abilitiesOff();
        })
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed when flying.")
        .defaultValue(0.1)
        .min(0.0)
        .build()
    );

    private final Setting<Boolean> verticalSpeedMatch = sgGeneral.add(new BoolSetting.Builder()
        .name("vertical-speed-match")
        .description("Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noSneak = sgGeneral.add(new BoolSetting.Builder()
        .name("no-sneak")
        .description("Prevents you from sneaking while flying.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Velocity)
        .build()
    );

    // Bypass settings
    private final Setting<Boolean> autoBypass = sgBypass.add(new BoolSetting.Builder()
        .name("auto-bypass")
        .description("Automatically adjusts settings based on detected anti-cheat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> grimBypass = sgBypass.add(new BoolSetting.Builder()
        .name("grim-bypass")
        .description("Enables GrimAC-specific flight bypass patterns.")
        .defaultValue(false)
        .visible(() -> !autoBypass.get())
        .build()
    );

    private final Setting<Boolean> vulcanBypass = sgBypass.add(new BoolSetting.Builder()
        .name("vulcan-bypass")
        .description("Enables Vulcan-specific flight bypass patterns.")
        .defaultValue(false)
        .visible(() -> !autoBypass.get())
        .build()
    );

    private final Setting<Boolean> matrixBypass = sgBypass.add(new BoolSetting.Builder()
        .name("matrix-bypass")
        .description("Enables Matrix-specific flight bypass patterns.")
        .defaultValue(false)
        .visible(() -> !autoBypass.get())
        .build()
    );

    private final Setting<Boolean> simulateGround = sgBypass.add(new BoolSetting.Builder()
        .name("simulate-ground")
        .description("Sends ground packets periodically to simulate ground state.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> groundSpoofInterval = sgBypass.add(new IntSetting.Builder()
        .name("ground-spoof-interval")
        .description("How often (in ticks) to spoof ground state.")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .visible(simulateGround::get)
        .build()
    );

    private final Setting<Boolean> randomYaw = sgBypass.add(new BoolSetting.Builder()
        .name("random-yaw-shift")
        .description("Adds small random yaw shifts to confuse rotation checks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> speedCap = sgBypass.add(new DoubleSetting.Builder()
        .name("speed-cap")
        .description("Maximum speed to prevent triggering movement checks. 0 = no cap.")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(10.0)
        .build()
    );

    // Anti Kick
    private final Setting<AntiKickMode> antiKickMode = sgAntiKick.add(new EnumSetting.Builder<AntiKickMode>()
        .name("mode")
        .description("The mode for anti kick.")
        .defaultValue(AntiKickMode.Packet)
        .build()
    );

    private final Setting<Integer> delay = sgAntiKick.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay, in ticks, between flying down a bit and return to original position")
        .defaultValue(20)
        .min(1)
        .sliderMax(200)
        .build()
    );

    private final Setting<Integer> offTime = sgAntiKick.add(new IntSetting.Builder()
        .name("off-time")
        .description("The amount of delay, in ticks, to fly down a bit to reset floating ticks.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private int delayLeft = delay.get();
    private int offLeft = offTime.get();
    private boolean flip;
    private float lastYaw;
    private double lastPacketY = Double.MAX_VALUE;
    private int tickCounter = 0;
    private final Random random = new Random();

    public Flight() {
        super(Categories.Movement, "flight", "FLYYYY! No Fall is recommended. Upgraded with anti-cheat bypass.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = true;
            if (mc.player.getAbilities().instabuild) return;
            mc.player.getAbilities().mayfly = true;
        }
        tickCounter = 0;
        delayLeft = delay.get();
        offLeft = offTime.get();
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            abilitiesOff();
        }
    }

    private boolean isBypassActive(String acName) {
        if (autoBypass.get()) {
            return AntiCheatDetector.INSTANCE.isDetected() &&
                   AntiCheatDetector.INSTANCE.getDetectedAC().name.equals(acName);
        }
        return switch (acName) {
            case "GrimAC" -> grimBypass.get();
            case "Vulcan" -> vulcanBypass.get();
            case "Matrix" -> matrixBypass.get();
            default -> false;
        };
    }

    private double getEffectiveSpeed() {
        double baseSpeed = speed.get();

        // Auto-bypass speed adjustment
        if (autoBypass.get() && AntiCheatDetector.INSTANCE.isDetected()) {
            double acSpeed = AntiCheatDetector.INSTANCE.getFlightSpeed();
            baseSpeed = Math.min(baseSpeed, acSpeed * 0.1); // Convert from blocks/tick scale
        }

        // Grim: very strict, need to limit speed significantly
        if (isBypassActive("GrimAC")) {
            baseSpeed = Math.min(baseSpeed, 0.05);
        }

        // Vulcan: moderate, allow slightly higher
        if (isBypassActive("Vulcan")) {
            baseSpeed = Math.min(baseSpeed, 0.15);
        }

        // Matrix: moderate strictness
        if (isBypassActive("Matrix")) {
            baseSpeed = Math.min(baseSpeed, 0.10);
        }

        // Apply speed cap
        if (speedCap.get() > 0) {
            baseSpeed = Math.min(baseSpeed, speedCap.get());
        }

        return baseSpeed;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        tickCounter++;
        float currentYaw = mc.player.getYRot();
        if (mc.player.fallDistance >= 3f && currentYaw == lastYaw && mc.player.getDeltaMovement().length() < 0.003d) {
            mc.player.setYRot(currentYaw + (flip ? 1 : -1));
            flip = !flip;
        }
        lastYaw = currentYaw;

        // Ground spoofing for anti-cheat bypass
        if (simulateGround.get() && tickCounter % groundSpoofInterval.get() == 0) {
            mc.player.setOnGround(true);
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (delayLeft > 0) delayLeft--;

        if (offLeft <= 0 && delayLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();

            if (antiKickMode.get() == AntiKickMode.Packet) {
                ((LocalPlayerAccessor) mc.player).meteor$setPositionReminder(20);
            }
        } else if (delayLeft <= 0) {
            boolean shouldReturn = false;

            if (antiKickMode.get() == AntiKickMode.Normal) {
                if (mode.get() == Mode.Abilities) {
                    abilitiesOff();
                    shouldReturn = true;
                }
            } else if (antiKickMode.get() == AntiKickMode.Packet && offLeft == offTime.get()) {
                ((LocalPlayerAccessor) mc.player).meteor$setPositionReminder(20);
            }

            offLeft--;

            if (shouldReturn) return;
        }

        if (mc.player.getYRot() != lastYaw) mc.player.setYRot(lastYaw);

        double effectiveSpeed = getEffectiveSpeed();

        switch (mode.get()) {
            case Velocity -> {
                mc.player.getAbilities().flying = false;
                mc.player.setDeltaMovement(0, 0, 0);
                Vec3 playerVelocity = mc.player.getDeltaMovement();

                double verticalMult = verticalSpeedMatch.get() ? 10f : 5f;
                if (mc.options.keyJump.isDown())
                    playerVelocity = playerVelocity.add(0, effectiveSpeed * verticalMult, 0);
                if (mc.options.keyShift.isDown())
                    playerVelocity = playerVelocity.subtract(0, effectiveSpeed * verticalMult, 0);

                mc.player.setDeltaMovement(playerVelocity);

                // Grim bypass: simulate small downward drift
                if (isBypassActive("GrimAC")) {
                    Vec3 vel = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(vel.x, vel.y - 0.005, vel.z);
                }

                // Vulcan bypass: intermittent ground state
                if (isBypassActive("Vulcan") && tickCounter % 3 == 0) {
                    mc.player.setOnGround(true);
                }

                if (noSneak.get()) {
                    mc.player.setOnGround(false);
                }
            }
            case Abilities -> {
                if (mc.player.isSpectator()) return;
                mc.player.getAbilities().setFlyingSpeed((float) effectiveSpeed);
                mc.player.getAbilities().flying = true;
                if (mc.player.getAbilities().instabuild) return;
                mc.player.getAbilities().mayfly = true;
            }
        }

        // Random yaw shifts to confuse rotation-based anti-cheat
        if (randomYaw.get() && tickCounter % 8 == 0) {
            float shift = (random.nextFloat() - 0.5f) * 2f;
            mc.player.setYRot(mc.player.getYRot() + shift);
        }
    }

    private void antiKickPacket(ServerboundMovePlayerPacket packet, double currentY) {
        if (this.delayLeft <= 0 && this.lastPacketY != Double.MAX_VALUE &&
            shouldFlyDown(currentY, this.lastPacketY) && EntityUtils.isOnAir(mc.player)) {
            ((ServerboundMovePlayerPacketAccessor) packet).meteor$setY(lastPacketY - 0.03130D);
        } else {
            lastPacketY = currentY;
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet) || antiKickMode.get() != AntiKickMode.Packet)
            return;

        double currentY = packet.getY(Double.MAX_VALUE);
        if (currentY != Double.MAX_VALUE) {
            antiKickPacket(packet, currentY);
        } else {
            ServerboundMovePlayerPacket fullPacket;
            if (packet.hasRotation()) {
                fullPacket = new ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    packet.getYRot(0),
                    packet.getXRot(0),
                    packet.isOnGround(),
                    mc.player.horizontalCollision
                );
            } else {
                fullPacket = new ServerboundMovePlayerPacket.Pos(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    packet.isOnGround(),
                    mc.player.horizontalCollision
                );
            }
            event.cancel();
            antiKickPacket(fullPacket, mc.player.getY());
            mc.getConnection().send(fullPacket);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundPlayerAbilitiesPacket packet) || mode.get() != Mode.Abilities) return;
        event.cancel();

        mc.player.getAbilities().invulnerable = packet.isInvulnerable();
        mc.player.getAbilities().instabuild = packet.canInstabuild();
        mc.player.getAbilities().setWalkingSpeed(packet.getWalkingSpeed());
    }

    private boolean shouldFlyDown(double currentY, double lastY) {
        if (currentY >= lastY) {
            return true;
        } else return lastY - currentY < 0.03130D;
    }

    private void abilitiesOff() {
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlyingSpeed(0.05f);
        if (mc.player.getAbilities().instabuild) return;
        mc.player.getAbilities().mayfly = false;
    }

    public float getFlyingSpeed() {
        if (!isActive() || mode.get() != Mode.Velocity) return -1;
        return (float) getEffectiveSpeed() * (mc.player.isSprinting() ? 15f : 10f);
    }

    public boolean noSneak() {
        return isActive() && mode.get() == Mode.Velocity && noSneak.get();
    }

    public enum Mode {
        Abilities,
        Velocity
    }

    public enum AntiKickMode {
        Normal,
        Packet,
        None
    }
}
