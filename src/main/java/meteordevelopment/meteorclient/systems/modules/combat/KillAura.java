/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 *
 * KillAura - Upgraded with rotation smoothing, random delays, and anti-cheat bypass.
 * Targets GrimAC, Vulcan, Matrix, Spartan, NCP rotation and attack speed checks.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.anticheat.AntiCheatDetector;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class KillAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgBypass = settings.createGroup("Bypass");

    // General

    private final Setting<AttackItems> attackWhenHolding = sgGeneral.add(new EnumSetting.Builder<AttackItems>()
        .name("attack-when-holding")
        .description("Only attacks an entity when a specified item is in your hand.")
        .defaultValue(AttackItems.Weapons)
        .build()
    );

    private final Setting<List<Item>> weapons = sgGeneral.add(new ItemListSetting.Builder()
        .name("selected-weapon-types")
        .description("Which types of weapons to attack with.")
        .defaultValue(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.TRIDENT)
        .filter(FILTER::contains)
        .visible(() -> attackWhenHolding.get() == AttackItems.Weapons)
        .build()
    );

    private final Setting<RotationMode> rotation = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("rotate")
        .description("Determines when you should rotate towards the target.")
        .defaultValue(RotationMode.Smooth)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches to an acceptable weapon when attacking the target.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches to your previous slot when done attacking the target.")
        .defaultValue(false)
        .visible(autoSwitch::get)
        .build()
    );

    private final Setting<ShieldMode> shieldMode = sgGeneral.add(new EnumSetting.Builder<ShieldMode>()
        .name("shield-mode")
        .description("What to do when your target is blocking with a shield.")
        .defaultValue(ShieldMode.None)
        .build()
    );

    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-click")
        .description("Only attacks when holding left click.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnLook = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-look")
        .description("Only attacks when looking at an entity.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("Freezes Baritone temporarily until you are finished attacking the entity.")
        .defaultValue(true)
        .build()
    );

    // Targeting

    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.ClosestAngle)
        .build()
    );

    private final Setting<Integer> maxTargets = sgTargeting.add(new IntSetting.Builder()
        .name("max-targets")
        .description("How many entities to target at once.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 5)
        .visible(() -> !onlyOnLook.get())
        .build()
    );

    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range the entity can be to attack it.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> wallsRange = sgTargeting.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("The maximum range the entity can be attacked through walls.")
        .defaultValue(3.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<EntityAge> passiveMobAgeFilter = sgTargeting.add(new EnumSetting.Builder<EntityAge>()
        .name("passive-mob-age-filter")
        .description("Determines the age of passive mobs to target.")
        .defaultValue(EntityAge.Adult)
        .build()
    );

    private final Setting<EntityAge> hostileMobAgeFilter = sgTargeting.add(new EnumSetting.Builder<EntityAge>()
        .name("hostile-mob-age-filter")
        .description("Determines the age of hostile mobs to target.")
        .defaultValue(EntityAge.Both)
        .build()
    );

    private final Setting<Boolean> ignoreNamed = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-named")
        .description("Whether or not to attack mobs with a name.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignorePassive = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-passive")
        .description("Will only attack sometimes passive mobs if they are targeting you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreTamed = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-tamed")
        .description("Will avoid attacking mobs you tamed.")
        .defaultValue(false)
        .build()
    );

    // Timing

    private final Setting<Boolean> pauseOnLag = sgTiming.add(new BoolSetting.Builder()
        .name("pause-on-lag")
        .description("Pauses if the server is lagging.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnUse = sgTiming.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Does not attack while using an item.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> tpsSync = sgTiming.add(new BoolSetting.Builder()
        .name("TPS-sync")
        .description("Tries to sync attack delay with the server's TPS.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> customDelay = sgTiming.add(new BoolSetting.Builder()
        .name("custom-delay")
        .description("Use a custom delay instead of the vanilla cooldown.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> hitDelay = sgTiming.add(new IntSetting.Builder()
        .name("hit-delay")
        .description("How fast you hit the entity in ticks.")
        .defaultValue(11)
        .min(0)
        .sliderMax(60)
        .visible(customDelay::get)
        .build()
    );

    private final Setting<Integer> switchDelay = sgTiming.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("How many ticks to wait before hitting an entity after switching hotbar slots.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build()
    );

    // Bypass settings
    private final Setting<Boolean> autoBypass = sgBypass.add(new BoolSetting.Builder()
        .name("auto-bypass")
        .description("Automatically adjusts attack timing and rotation for detected anti-cheat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smoothRotations = sgBypass.add(new BoolSetting.Builder()
        .name("smooth-rotations")
        .description("Smoothly rotates towards targets instead of snapping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> rotationSpeed = sgBypass.add(new DoubleSetting.Builder()
        .name("rotation-speed")
        .description("How fast to rotate (degrees per tick). Lower = more legit.")
        .defaultValue(50.0)
        .min(1.0)
        .sliderMax(180.0)
        .visible(smoothRotations::get)
        .build()
    );

    private final Setting<Boolean> randomizeDelay = sgBypass.add(new BoolSetting.Builder()
        .name("randomize-delay")
        .description("Adds random variation to attack delays to avoid pattern detection.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> randomDelayMin = sgBypass.add(new IntSetting.Builder()
        .name("random-delay-min")
        .description("Minimum random delay addition in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .visible(randomizeDelay::get)
        .build()
    );

    private final Setting<Integer> randomDelayMax = sgBypass.add(new IntSetting.Builder()
        .name("random-delay-max")
        .description("Maximum random delay addition in ticks.")
        .defaultValue(5)
        .min(0)
        .sliderMax(20)
        .visible(randomizeDelay::get)
        .build()
    );

    private final Setting<Boolean> microJitter = sgBypass.add(new BoolSetting.Builder()
        .name("micro-jitter")
        .description("Adds tiny random jitter to rotation angles for realism.")
        .defaultValue(false)
        .build()
    );

    private final static ArrayList<Item> FILTER = new ArrayList<>(List.of(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.MACE, Items.DIAMOND_SPEAR, Items.TRIDENT));
    private final List<Entity> targets = new ArrayList<>();
    private int switchTimer, hitTimer, bypassDelay;
    private boolean wasPathing = false;
    public boolean attacking, swapped;
    public static int previousSlot;
    private final Random random = new Random();

    // Smooth rotation state
    private float targetYaw, targetPitch;
    private float currentYaw, currentPitch;
    private boolean rotating = false;

    public KillAura() {
        super(Categories.Combat, "kill-aura", "Attacks specified entities around you. Upgraded with anti-cheat bypass.");
    }

    @Override
    public void onActivate() {
        previousSlot = -1;
        swapped = false;
        bypassDelay = 0;
        rotating = false;
    }

    @Override
    public void onDeactivate() {
        targets.clear();
        stopAttacking();
        rotating = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameType.SPECTATOR) {
            stopAttacking();
            return;
        }
        if (pauseOnUse.get() && (mc.gameMode.isDestroying() || mc.player.isUsingItem())) {
            stopAttacking();
            return;
        }
        if (onlyOnClick.get() && !mc.options.keyAttack.isDown()) {
            stopAttacking();
            return;
        }
        if (TickRate.INSTANCE.getTimeSinceLastTick() >= 1f && pauseOnLag.get()) {
            stopAttacking();
            return;
        }

        if (onlyOnLook.get()) {
            Entity targeted = mc.crosshairPickEntity;
            if (targeted == null || !entityCheck(targeted)) {
                stopAttacking();
                return;
            }
            targets.clear();
            targets.add(mc.crosshairPickEntity);
        } else {
            targets.clear();
            TargetUtils.getList(targets, this::entityCheck, priority.get(), maxTargets.get());
        }

        if (targets.isEmpty()) {
            stopAttacking();
            return;
        }

        Entity primary = targets.getFirst();

        if (autoSwitch.get()) {
            FindItemResult weaponResult = new FindItemResult(mc.player.getInventory().getSelectedSlot(), -1);
            if (attackWhenHolding.get() == AttackItems.Weapons)
                weaponResult = InvUtils.find(this::acceptableWeapon, 0, 8);

            if (shouldShieldBreak()) {
                FindItemResult axeResult = InvUtils.find(itemStack -> itemStack.getItem() instanceof AxeItem, 0, 8);
                if (axeResult.found()) weaponResult = axeResult;
            }

            if (!swapped) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                swapped = true;
            }

            InvUtils.swap(weaponResult.slot(), false);
        }

        if (!acceptableWeapon(mc.player.getMainHandItem())) {
            stopAttacking();
            return;
        }

        attacking = true;

        // Smooth rotation handling
        double desiredYaw = Rotations.getYaw(primary);
        double desiredPitch = Rotations.getPitch(primary, Target.Body);

        if (smoothRotations.get()) {
            currentYaw = mc.player.getYRot();
            currentPitch = mc.player.getXRot();

            // Calculate rotation delta with speed limit
            float yawDiff = Mth.wrapDegrees((float)(desiredYaw - currentYaw));
            float pitchDiff = Mth.wrapDegrees((float)(desiredPitch - currentPitch));

            float maxSpeed = rotationSpeed.get().floatValue();

            // Auto-bypass: slow down rotations for strict anti-cheats
            if (autoBypass.get() && AntiCheatDetector.INSTANCE.isDetected()) {
                int[] delayRange = AntiCheatDetector.INSTANCE.getKillAuraDelayRange();
                maxSpeed = Math.min(maxSpeed, 30f + (delayRange[1] - delayRange[0]) * 0.05f);
            }

            float yawStep = Mth.clamp(yawDiff, -maxSpeed, maxSpeed);
            float pitchStep = Mth.clamp(pitchDiff, -maxSpeed, maxSpeed);

            targetYaw = Mth.wrapDegrees(currentYaw + yawStep);
            targetPitch = Mth.clamp(currentPitch + pitchStep, -90f, 90f);

            // Add micro-jitter for realism
            if (microJitter.get()) {
                targetYaw += (random.nextFloat() - 0.5f) * 1.5f;
                targetPitch += (random.nextFloat() - 0.5f) * 0.8f;
            }

            if (rotation.get() == RotationMode.Smooth || rotation.get() == RotationMode.Always) {
                Rotations.rotate(targetYaw, targetPitch);
            }

            // Check if rotation is close enough to attack
            float yawRemaining = Math.abs(Mth.wrapDegrees((float)(desiredYaw - targetYaw)));
            float pitchRemaining = Math.abs(Mth.wrapDegrees((float)(desiredPitch - targetPitch)));

            if (yawRemaining > 30f || pitchRemaining > 15f) {
                // Still rotating, don't attack yet
                if (pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
                    PathManagers.get().pause();
                    wasPathing = true;
                }
                return;
            }
        } else {
            if (rotation.get() == RotationMode.Always || rotation.get() == RotationMode.Smooth)
                Rotations.rotate(Rotations.getYaw(primary), Rotations.getPitch(primary, Target.Body));
        }

        if (pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
            PathManagers.get().pause();
            wasPathing = true;
        }

        // Handle bypass delay
        if (bypassDelay > 0) {
            bypassDelay--;
            return;
        }

        if (delayCheck()) targets.forEach(this::attack);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundSetCarriedItemPacket) {
            switchTimer = switchDelay.get();
        }
    }

    private void stopAttacking() {
        if (!attacking) return;

        attacking = false;
        rotating = false;
        if (wasPathing) {
            PathManagers.get().resume();
            wasPathing = false;
        }
        if (swapBack.get() && swapped) {
            InvUtils.swap(previousSlot, false);
            swapped = false;
        }
    }

    private boolean shouldShieldBreak() {
        for (Entity target : targets) {
            if (target instanceof Player player) {
                if (player.isBlocking() && shieldMode.get() == ShieldMode.Break) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity())) return false;
        if ((entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) || !entity.isAlive())
            return false;

        AABB hitbox = entity.getBoundingBox();
        if (!PlayerUtils.isWithin(
            Mth.clamp(mc.player.getX(), hitbox.minX, hitbox.maxX),
            Mth.clamp(mc.player.getY(), hitbox.minY, hitbox.maxY),
            Mth.clamp(mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
            range.get()
        )) return false;

        if (!entities.get().contains(entity.getType())) return false;
        if (ignoreNamed.get() && entity.hasCustomName()) return false;
        if (!PlayerUtils.canSeeEntity(entity) && !PlayerUtils.isWithin(entity, wallsRange.get())) return false;
        if (ignoreTamed.get()) {
            if (entity instanceof OwnableEntity tameable
                && tameable.getOwner() != null
                && tameable.getOwner().equals(mc.player)
            ) return false;
        }
        if (ignorePassive.get()) {
            if (entity instanceof EnderMan enderman && !enderman.isAngry()) return false;
            if ((entity instanceof Piglin || entity instanceof ZombifiedPiglin || entity instanceof Wolf) && !((Mob) entity).isAggressive())
                return false;
        }
        if (entity instanceof Player player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
            if (shieldMode.get() == ShieldMode.Ignore && player.isBlocking()) return false;
            if (player instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit) return false;
        }
        if (entity instanceof LivingEntity livingEntity) {
            if (entity instanceof Zombie || entity instanceof Piglin
                || entity instanceof Hoglin || entity instanceof Zoglin) {
                return switch (hostileMobAgeFilter.get()) {
                    case Baby -> livingEntity.isBaby();
                    case Adult -> !livingEntity.isBaby();
                    case Both -> true;
                };
            }
            if (entity instanceof AgeableMob && (!(entity instanceof Frog || entity instanceof Parrot))) {
                return switch (passiveMobAgeFilter.get()) {
                    case Baby -> livingEntity.isBaby();
                    case Adult -> !livingEntity.isBaby();
                    case Both -> true;
                };
            }
        }
        return true;
    }

    private boolean delayCheck() {
        if (switchTimer > 0) {
            switchTimer--;
            return false;
        }

        float delay = (customDelay.get()) ? hitDelay.get() : 0.5f;
        if (tpsSync.get()) delay /= (TickRate.INSTANCE.getTickRate() / 20);

        // Auto-bypass: adjust delay based on anti-cheat
        if (autoBypass.get() && AntiCheatDetector.INSTANCE.isDetected()) {
            int[] acDelayRange = AntiCheatDetector.INSTANCE.getKillAuraDelayRange();
            int acBaseDelay = (acDelayRange[0] + acDelayRange[1]) / 2;
            if (customDelay.get()) {
                delay = Math.max(delay, acBaseDelay);
            } else {
                // Force a minimum delay for anti-cheat compliance
                delay = Math.max(delay, acBaseDelay / 20f);
            }
        }

        if (customDelay.get()) {
            if (hitTimer < delay) {
                hitTimer++;
                return false;
            } else return true;
        } else return mc.player.getAttackStrengthScale(delay) >= 1;
    }

    private void attack(Entity target) {
        if (rotation.get() == RotationMode.OnHit)
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body));

        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);

        hitTimer = 0;

        // Add random bypass delay after attack
        if (randomizeDelay.get()) {
            int min = randomDelayMin.get();
            int max = Math.max(randomDelayMax.get(), min);
            bypassDelay = min + random.nextInt(max - min + 1);
        }

        // Auto-bypass: add AC-specific delay
        if (autoBypass.get() && AntiCheatDetector.INSTANCE.isDetected()) {
            int[] acDelayRange = AntiCheatDetector.INSTANCE.getKillAuraDelayRange();
            int acExtra = acDelayRange[0] + random.nextInt(acDelayRange[1] - acDelayRange[0] + 1);
            bypassDelay = Math.max(bypassDelay, acExtra / 50); // Convert ms to approximate ticks
        }
    }

    private boolean acceptableWeapon(ItemStack stack) {
        if (shouldShieldBreak()) return stack.getItem() instanceof AxeItem;
        if (attackWhenHolding.get() == AttackItems.All) return true;

        if (weapons.get().contains(Items.DIAMOND_SWORD) && stack.is(ItemTags.SWORDS)) return true;
        if (weapons.get().contains(Items.DIAMOND_AXE) && stack.is(ItemTags.AXES)) return true;
        if (weapons.get().contains(Items.DIAMOND_PICKAXE) && stack.is(ItemTags.PICKAXES)) return true;
        if (weapons.get().contains(Items.DIAMOND_SHOVEL) && stack.is(ItemTags.SHOVELS)) return true;
        if (weapons.get().contains(Items.DIAMOND_HOE) && stack.is(ItemTags.HOES)) return true;
        if (weapons.get().contains(Items.MACE) && stack.getItem() instanceof MaceItem) return true;
        if (weapons.get().contains(Items.DIAMOND_SPEAR) && stack.is(ItemTags.SPEARS)) return true;
        return weapons.get().contains(Items.TRIDENT) && stack.getItem() instanceof TridentItem;
    }

    public Entity getTarget() {
        if (!targets.isEmpty()) return targets.getFirst();
        return null;
    }

    @Override
    public String getInfoString() {
        if (!targets.isEmpty()) return EntityUtils.getName(getTarget());
        return null;
    }

    public enum AttackItems {
        Weapons,
        All
    }

    public enum RotationMode {
        Always,
        OnHit,
        Smooth,
        None
    }

    public enum ShieldMode {
        Ignore,
        Break,
        None
    }

    public enum EntityAge {
        Baby,
        Adult,
        Both
    }
}
