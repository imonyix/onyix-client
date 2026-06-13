/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 *
 * AntiCheatDetector - Automatically detects server anti-cheat and adjusts module settings.
 * Supported detection: GrimAC, Vulcan, Matrix, Spartan, NoCheatPlus
 */

package meteordevelopment.meteorclient.systems.anticheat;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

import java.util.regex.Pattern;

public enum AntiCheatDetector {
    INSTANCE;

    public enum ServerAntiCheat {
        None("None"),
        GrimAC("GrimAC"),
        Vulcan("Vulcan"),
        Matrix("Matrix"),
        Spartan("Spartan"),
        NoCheatPlus("NoCheatPlus"),
        Unknown("Unknown");

        public final String name;

        ServerAntiCheat(String name) {
            this.name = name;
        }
    }

    private ServerAntiCheat detectedAC = ServerAntiCheat.None;
    private boolean detected = false;
    private int tickCounter = 0;

    // Patterns for detection from chat messages / kick messages
    private static final Pattern GRIM_PATTERN = Pattern.compile("(?i)grim|grimmanticheat|grimac");
    private static final Pattern VULCAN_PATTERN = Pattern.compile("(?i)vulcan|vulcananticheat");
    private static final Pattern MATRIX_PATTERN = Pattern.compile("(?i)matrix|matrixanticheat");
    private static final Pattern SPARTAN_PATTERN = Pattern.compile("(?i)spartan|spartananticheat");
    private static final Pattern NCP_PATTERN = Pattern.compile("(?i)nocheatplus|ncp");

    public void init() {
        MeteorClient.EVENT_BUS.subscribe(this);
        MeteorClient.LOG.info("[Onyix] AntiCheatDetector initialized");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!detected) {
            tickCounter++;
            // Try to detect anti-cheat from server brand after 40 ticks (~2 seconds)
            if (tickCounter == 40) {
                tryDetectFromBrand();
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        // Detect from kick messages
        if (event.packet instanceof ClientboundDisconnectPacket packet) {
            String reason = packet.reason().getString();
            checkForAntiCheat(reason);
        }
    }

    private void tryDetectFromBrand() {
        try {
            if (MeteorClient.mc.player != null && MeteorClient.mc.getConnection() != null) {
                String brand = MeteorClient.mc.getConnection().serverBrand();
                if (brand == null) brand = "";
                checkForAntiCheat(brand);
            }
        } catch (Exception ignored) {
            // Brand detection is not always available
        }
    }

    private void checkForAntiCheat(String text) {
        if (text == null || text.isEmpty()) return;

        if (GRIM_PATTERN.matcher(text).find()) {
            setDetected(ServerAntiCheat.GrimAC);
        } else if (VULCAN_PATTERN.matcher(text).find()) {
            setDetected(ServerAntiCheat.Vulcan);
        } else if (MATRIX_PATTERN.matcher(text).find()) {
            setDetected(ServerAntiCheat.Matrix);
        } else if (SPARTAN_PATTERN.matcher(text).find()) {
            setDetected(ServerAntiCheat.Spartan);
        } else if (NCP_PATTERN.matcher(text).find()) {
            setDetected(ServerAntiCheat.NoCheatPlus);
        }
    }

    private void setDetected(ServerAntiCheat ac) {
        if (detected && detectedAC == ac) return;
        this.detectedAC = ac;
        this.detected = true;
        MeteorClient.LOG.info("[Onyix] Detected anti-cheat: {}", ac.name);

        // Notify player
        if (MeteorClient.mc.player != null) {
            MeteorClient.mc.player.sendSystemMessage(
                Component.literal("§b[Onyix] §7Detected anti-cheat: §f" + ac.name)
            );
        }
    }

    public ServerAntiCheat getDetectedAC() {
        return detectedAC;
    }

    public boolean isDetected() {
        return detected;
    }

    public void reset() {
        detectedAC = ServerAntiCheat.None;
        detected = false;
        tickCounter = 0;
    }

    // Bypass configuration helpers
    public boolean isGrim() { return detectedAC == ServerAntiCheat.GrimAC; }
    public boolean isVulcan() { return detectedAC == ServerAntiCheat.Vulcan; }
    public boolean isMatrix() { return detectedAC == ServerAntiCheat.Matrix; }
    public boolean isSpartan() { return detectedAC == ServerAntiCheat.Spartan; }
    public boolean isNCP() { return detectedAC == ServerAntiCheat.NoCheatPlus; }

    // Get recommended flight speed based on detected AC
    public double getFlightSpeed() {
        return switch (detectedAC) {
            case GrimAC -> 0.5;     // Grim is very strict on flight
            case Vulcan -> 1.5;     // Vulcan moderate
            case Matrix -> 1.0;    // Matrix moderate
            case Spartan -> 2.0;   // Spartan lenient
            case NoCheatPlus -> 0.8; // NCP strict
            default -> 5.0;       // No AC detected - full speed
        };
    }

    // Get recommended speed multiplier
    public double getSpeedMultiplier() {
        return switch (detectedAC) {
            case GrimAC -> 1.2;
            case Vulcan -> 1.5;
            case Matrix -> 1.3;
            case Spartan -> 1.8;
            case NoCheatPlus -> 1.1;
            default -> 2.0;
        };
    }

    // Get recommended velocity reduction
    public double getVelocityReduction() {
        return switch (detectedAC) {
            case GrimAC -> 0.90;   // Grim checks ground/air velocity patterns
            case Vulcan -> 0.85;
            case Matrix -> 0.88;
            case Spartan -> 0.80;
            case NoCheatPlus -> 0.95;
            default -> 0.0;
        };
    }

    // Get KillAura delay range (min-max ms)
    public int[] getKillAuraDelayRange() {
        return switch (detectedAC) {
            case GrimAC -> new int[]{150, 400};  // Grim rotation checks
            case Vulcan -> new int[]{100, 300};
            case Matrix -> new int[]{120, 350};
            case Spartan -> new int[]{80, 250};
            case NoCheatPlus -> new int[]{200, 500};
            default -> new int[]{0, 50};
        };
    }

    // NoSlow factor
    public double getNoSlowFactor() {
        return switch (detectedAC) {
            case GrimAC -> 0.8;     // Grim 80% speed retention
            case Vulcan -> 0.85;
            case Matrix -> 0.75;
            case Spartan -> 0.9;
            case NoCheatPlus -> 0.7;
            default -> 1.0;
        };
    }
}
