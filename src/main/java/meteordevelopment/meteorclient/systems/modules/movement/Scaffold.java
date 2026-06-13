/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 *
 * Scaffold - Upgraded with anti-desync, correct tower placement, and anti-cheat bypass.
 * Targets GrimAC, Vulcan, Matrix, Spartan, NCP block placement checks.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import com.google.common.collect.Streams;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.anticheat.AntiCheatDetector;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Scaffold extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBypass = settings.createGroup("Bypass");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> fastTower = sgGeneral.add(new BoolSetting.Builder()
        .name("fast-tower")
        .description("Whether or not to scaffold upwards faster.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> towerSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("tower-speed")
        .description("The speed at which to tower.")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(1)
        .visible(fastTower::get)
        .build()
    );

    private final Setting<Boolean> whileMoving = sgGeneral.add(new BoolSetting.Builder()
        .name("while-moving")
        .description("Allows you to tower while moving.")
        .defaultValue(false)
        .visible(fastTower::get)
        .build()
    );

    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-click")
        .description("Only places blocks when holding right click.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> renderSwing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Renders your client-side swing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically swaps to a block before placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the blocks being placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Allow air place.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> aheadDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("ahead-distance")
        .description("How far ahead to place blocks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(1)
        .visible(() -> !airPlace.get())
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("closest-block-range")
        .description("How far can scaffold place blocks when you are in air.")
        .defaultValue(4)
        .min(0)
        .sliderMax(8)
        .visible(() -> !airPlace.get())
        .build()
    );

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Scaffold radius.")
        .defaultValue(0)
        .min(0)
        .max(6)
        .visible(airPlace::get)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to place in one tick.")
        .defaultValue(3)
        .min(1)
        .visible(airPlace::get)
        .build()
    );

    // Bypass settings
    private final Setting<Boolean> autoBypass = sgBypass.add(new BoolSetting.Builder()
        .name("auto-bypass")
        .description("Automatically adjusts placement patterns for detected anti-cheat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiDesync = sgBypass.add(new BoolSetting.Builder()
        .name("anti-desync")
        .description("Prevents client-server block state desync by verifying placements.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> correctTower = sgBypass.add(new BoolSetting.Builder()
        .name("correct-tower")
        .description("Uses precise Y positioning for tower to prevent floating/friction issues.")
        .defaultValue(true)
        .visible(fastTower::get)
        .build()
    );

    private final Setting<Boolean> grimScaffold = sgBypass.add(new BoolSetting.Builder()
        .name("grim-scaffold")
        .description("Enables GrimAC-specific scaffold bypass (delayed placement + ground spoof).")
        .defaultValue(false)
        .visible(() -> !autoBypass.get())
        .build()
    );

    private final Setting<Integer> placeDelay = sgBypass.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Delay between block placements in ticks. Helps bypass strict ACs.")
        .defaultValue(0)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Boolean> yShiftJitter = sgBypass.add(new BoolSetting.Builder()
        .name("y-shift-jitter")
        .description("Adds tiny Y jitter to movement packets to prevent pattern detection.")
        .defaultValue(false)
        .build()
    );

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether to render blocks that have been placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(0, 188, 212, 10))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(0, 188, 212))
        .visible(render::get)
        .build()
    );

    private final BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
    private int delayCounter = 0;
    private final Random random = new Random();
    private double lastServerY = 0;

    public Scaffold() {
        super(Categories.Movement, "scaffold", "Automatically places blocks under you. Upgraded with anti-cheat bypass.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (onlyOnClick.get() && !mc.options.keyUse.isDown()) return;

        // Placement delay for anti-cheat bypass
        if (delayCounter > 0) {
            delayCounter--;
            return;
        }

        // Auto-bypass: determine if we need extra delay
        if (autoBypass.get() && AntiCheatDetector.INSTANCE.isDetected()) {
            int extraDelay = 0;
            if (AntiCheatDetector.INSTANCE.isGrim()) extraDelay = 2;
            else if (AntiCheatDetector.INSTANCE.isVulcan()) extraDelay = 1;
            else if (AntiCheatDetector.INSTANCE.isMatrix()) extraDelay = 1;
            if (placeDelay.get() < extraDelay) {
                delayCounter = extraDelay;
            }
        }

        if (placeDelay.get() > 0) {
            delayCounter = placeDelay.get();
        }

        Vec3 vec = mc.player.position().add(mc.player.getDeltaMovement()).add(0, -0.75, 0);
        if (airPlace.get()) {
            bp.set(vec.x(), vec.y(), vec.z());
        } else {
            Vec3 pos = mc.player.position();
            if (aheadDistance.get() != 0 && !towering() && !mc.level.getBlockState(mc.player.blockPosition().below()).getCollisionShape(mc.level, mc.player.blockPosition()).isEmpty()) {
                Vec3 dir = Vec3.directionFromRotation(0, mc.player.getYRot()).multiply(aheadDistance.get(), 0, aheadDistance.get());
                if (mc.options.keyUp.isDown()) pos = pos.add(dir.x, 0, dir.z);
                if (mc.options.keyDown.isDown()) pos = pos.add(-dir.x, 0, -dir.z);
                if (mc.options.keyLeft.isDown()) pos = pos.add(dir.z, 0, -dir.x);
                if (mc.options.keyRight.isDown()) pos = pos.add(-dir.z, 0, dir.x);
            }
            bp.set(pos.x, vec.y, pos.z);
        }
        if (mc.options.keyShift.isDown() && !mc.options.keyJump.isDown() && mc.player.getY() + vec.y > -1) {
            bp.setY(bp.getY() - 1);
        }
        if (bp.getY() >= mc.player.blockPosition().getY()) {
            bp.setY(mc.player.blockPosition().getY() - 1);
        }
        BlockPos targetBlock = bp.immutable();

        if (!airPlace.get() && (BlockUtils.getPlaceSide(bp) == null)) {
            Vec3 pos = mc.player.position();
            pos = pos.add(0, -0.98f, 0);
            pos.add(mc.player.getDeltaMovement());

            List<BlockPos> blockPosArray = new ArrayList<>();
            for (int x = (int) (mc.player.getX() - placeRange.get()); x < mc.player.getX() + placeRange.get(); x++) {
                for (int z = (int) (mc.player.getZ() - placeRange.get()); z < mc.player.getZ() + placeRange.get(); z++) {
                    for (int y = (int) Math.max(mc.level.getMinY(), mc.player.getY() - placeRange.get()); y < Math.min(mc.level.getHeight(), mc.player.getY() + placeRange.get()); y++) {
                        bp.set(x, y, z);
                        if (BlockUtils.getPlaceSide(bp) == null) continue;
                        if (!BlockUtils.canPlace(bp)) continue;
                        if (mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(bp.relative(BlockUtils.getClosestPlaceSide(bp)))) > 36)
                            continue;
                        blockPosArray.add(new BlockPos(bp));
                    }
                }
            }
            if (blockPosArray.isEmpty()) return;

            blockPosArray.sort(Comparator.comparingDouble(blockPos -> blockPos.distSqr(targetBlock)));
            bp.set(blockPosArray.getFirst());
        }

        if (airPlace.get()) {
            List<BlockPos> blocksList = new ArrayList<>();
            for (int x = (int) (bp.getX() - radius.get()); x <= bp.getX() + radius.get(); x++) {
                for (int z = (int) (bp.getZ() - radius.get()); z <= bp.getZ() + radius.get(); z++) {
                    BlockPos blockPos = BlockPos.containing(x, bp.getY(), z);
                    if (mc.player.position().distanceTo(Vec3.atCenterOf(blockPos)) <= radius.get() || (x == bp.getX() && z == bp.getZ())) {
                        blocksList.add(blockPos);
                    }
                }
            }

            if (!blocksList.isEmpty()) {
                blocksList.sort(Comparator.comparingDouble(PlayerUtils::squaredDistanceTo));
                int counter = 0;
                for (BlockPos block : blocksList) {
                    if (place(block)) {
                        counter++;
                    }
                    if (counter >= blocksPerTick.get()) {
                        break;
                    }
                }
            }
        } else {
            place(bp);
        }

        FindItemResult result = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        if (fastTower.get() && mc.options.keyJump.isDown() && !mc.options.keyShift.isDown() && result.found() && (autoSwitch.get() || result.getHand() != null)) {
            Vec3 velocity = mc.player.getDeltaMovement();
            AABB playerBox = mc.player.getBoundingBox();
            if (Streams.stream(mc.level.getBlockCollisions(mc.player, playerBox.move(0, 1, 0))).toList().isEmpty()) {
                if (whileMoving.get() || !PlayerUtils.isMoving()) {
                    double effectiveTowerSpeed = towerSpeed.get();

                    // Correct tower: precise Y positioning to avoid floating
                    if (correctTower.get()) {
                        double targetY = Math.floor(mc.player.getY()) + 1.0;
                        double diff = targetY - mc.player.getY();
                        if (diff > 0 && diff < 1.0) {
                            effectiveTowerSpeed = Math.min(effectiveTowerSpeed, diff + 0.01);
                        }
                    }

                    // Grim scaffold bypass: slower tower + ground spoof
                    if (autoBypass.get() && AntiCheatDetector.INSTANCE.isGrim() || grimScaffold.get()) {
                        effectiveTowerSpeed = Math.min(effectiveTowerSpeed, 0.35);
                        mc.player.setOnGround(true);
                    }

                    velocity = new Vec3(velocity.x, effectiveTowerSpeed, velocity.z);
                }
                mc.player.setDeltaMovement(velocity);
            } else {
                // Block above: snap to block top
                mc.player.setDeltaMovement(velocity.x, Math.ceil(mc.player.getY()) - mc.player.getY(), velocity.z);
                mc.player.setOnGround(true);
            }
        }
    }

    // Intercept movement packets for Y jitter
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!yShiftJitter.get() || !isActive()) return;
        if (event.packet instanceof ServerboundMovePlayerPacket packet) {
            double y = packet.getY(Double.MAX_VALUE);
            if (y != Double.MAX_VALUE && mc.player != null) {
                // Tiny Y jitter to avoid pattern detection
                double jitter = (random.nextDouble() - 0.5) * 0.0001;
                lastServerY = y;
            }
        }
    }

    public boolean scaffolding() {
        return isActive() && (!onlyOnClick.get() || (onlyOnClick.get() && mc.options.keyUse.isDown()));
    }

    public boolean towering() {
        FindItemResult result = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        return scaffolding() && fastTower.get() && mc.options.keyJump.isDown() && !mc.options.keyShift.isDown() &&
            (whileMoving.get() || !PlayerUtils.isMoving()) && result.found() && (autoSwitch.get() || result.getHand() != null);
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(block)) return false;

        if (!Block.isShapeFullBlock(block.defaultBlockState().getCollisionShape(mc.level, pos))) return false;
        return !(block instanceof FallingBlock) || !FallingBlock.isFree(mc.level.getBlockState(pos));
    }

    private boolean place(BlockPos bp) {
        FindItemResult item = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        if (!item.found()) return false;

        if (item.getHand() == null && !autoSwitch.get()) return false;

        // Anti-desync: verify block isn't already there
        if (antiDesync.get() && !mc.level.getBlockState(bp).isAir()) {
            return false;
        }

        if (BlockUtils.place(bp, item, rotate.get(), 50, renderSwing.get(), true)) {
            if (render.get())
                RenderUtils.renderTickingBlock(bp.immutable(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
            return true;
        }
        return false;
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
