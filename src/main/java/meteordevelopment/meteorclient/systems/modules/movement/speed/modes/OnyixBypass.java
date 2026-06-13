/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 *
 * OnyixBypass - Speed mode with automatic anti-cheat bypass.
 * Adjusts movement patterns based on detected server anti-cheat.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.anticheat.AntiCheatDetector;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedMode;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;

import java.util.Random;

public class OnyixBypass extends SpeedMode {
    private final Random random = new Random();
    private int tickCounter = 0;
    private boolean wasOnGround = false;
    private double lastSpeed = 0.0;

    public OnyixBypass() {
        super(SpeedModes.OnyixBypass);
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        wasOnGround = false;
        lastSpeed = 0.0;
    }

    @Override
    public void onDeactivate() {
        tickCounter = 0;
    }

    @Override
    public void onTick() {
        tickCounter++;
        wasOnGround = mc.player.onGround();
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        if (!PlayerUtils.isMoving()) return;

        Speed speed = Modules.get().get(Speed.class);
        double baseMultiplier = speed.speedMode.get() == SpeedModes.OnyixBypass
            ? AntiCheatDetector.INSTANCE.getSpeedMultiplier()
            : 1.5;

        double maxSpeed = baseMultiplier * 0.287; // vanilla walk speed base
        IVec3 movement = (IVec3) event.movement;

        // GrimAC bypass: strict movement, use small burst + ground spoof
        if (AntiCheatDetector.INSTANCE.isGrim()) {
            maxSpeed = Math.min(maxSpeed, 0.34); // Grim threshold ~0.36
            if (mc.player.onGround()) {
                // Ground start boost
                movement.meteor$setY(getHop(0.40123128));
                mc.player.setOnGround(false);
            } else {
                // In-air: reduce horizontal to stay under Grim's threshold
                double currentSpeed = Math.sqrt(event.movement.x * event.movement.x + event.movement.z * event.movement.z);
                if (currentSpeed > maxSpeed) {
                    double scale = maxSpeed / currentSpeed;
                    movement.meteor$setXZ(event.movement.x * scale, event.movement.z * scale);
                }
            }
        }

        // Vulcan bypass: burst with timer pauses
        else if (AntiCheatDetector.INSTANCE.isVulcan()) {
            maxSpeed = Math.min(maxSpeed, 0.45);
            if (mc.player.onGround() && tickCounter % 2 == 0) {
                movement.meteor$setY(getHop(0.40123128));
            }
            double currentSpeed = Math.sqrt(event.movement.x * event.movement.x + event.movement.z * event.movement.z);
            if (currentSpeed > maxSpeed) {
                double scale = maxSpeed / currentSpeed;
                movement.meteor$setXZ(event.movement.x * scale, event.movement.z * scale);
            }
        }

        // Matrix bypass: friction-based
        else if (AntiCheatDetector.INSTANCE.isMatrix()) {
            maxSpeed = Math.min(maxSpeed, 0.38);
            if (mc.player.onGround()) {
                movement.meteor$setY(getHop(0.40123128));
                double boost = maxSpeed * (1.0 + random.nextDouble() * 0.05); // slight randomization
                double yaw = Math.toRadians(mc.player.getYRot());
                movement.meteor$setXZ(-Math.sin(yaw) * boost, Math.cos(yaw) * boost);
            } else {
                double currentSpeed = Math.sqrt(event.movement.x * event.movement.x + event.movement.z * event.movement.z);
                double friction = 0.91;
                double target = lastSpeed * friction + 0.013;
                if (currentSpeed > target) {
                    double scale = target / currentSpeed;
                    movement.meteor$setXZ(event.movement.x * scale, event.movement.z * scale);
                }
            }
        }

        // Spartan bypass: moderate checks
        else if (AntiCheatDetector.INSTANCE.isSpartan()) {
            maxSpeed = Math.min(maxSpeed, 0.52);
            if (mc.player.onGround() && tickCounter % 3 == 0) {
                movement.meteor$setY(getHop(0.40123128));
            }
        }

        // NCP bypass: standard strafe
        else if (AntiCheatDetector.INSTANCE.isNCP()) {
            maxSpeed = Math.min(maxSpeed, 0.34);
            if (mc.player.onGround()) {
                movement.meteor$setY(getHop(0.40123128));
            }
        }

        // No AC: full speed
        else {
            maxSpeed = baseMultiplier * 0.287;
            if (mc.player.onGround()) {
                movement.meteor$setY(getHop(0.40123128));
            }
            double yaw = Math.toRadians(mc.player.getYRot());
            double boost = maxSpeed;
            movement.meteor$setXZ(-Math.sin(yaw) * boost, Math.cos(yaw) * boost);
        }

        lastSpeed = Math.sqrt(event.movement.x * event.movement.x + event.movement.z * event.movement.z);
    }

    @Override
    public void onRubberband() {
        lastSpeed = 0.0;
    }

    @Override
    public String getHudString() {
        String ac = AntiCheatDetector.INSTANCE.isDetected() ? AntiCheatDetector.INSTANCE.getDetectedAC().name : "None";
        return "Onyix [" + ac + "]";
    }
}
