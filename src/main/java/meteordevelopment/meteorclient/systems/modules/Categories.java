/*
 * This file is part of the Onyix Client distribution.
 * Copyright (c) Onyix. Licensed under GPL-3.0.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.world.item.Items;

public class Categories {
    public static final Category Combat = new Category("Combat", () -> DisplayItemUtils.toStack(Items.GOLDEN_SWORD));
    public static final Category Player = new Category("Player", () -> DisplayItemUtils.toStack(Items.ARMOR_STAND));
    public static final Category Movement = new Category("Movement", () -> DisplayItemUtils.toStack(Items.DIAMOND_BOOTS));
    public static final Category Render = new Category("Render", () -> DisplayItemUtils.toStack(Items.GLASS));
    public static final Category World = new Category("World", () -> DisplayItemUtils.toStack(Items.GRASS_BLOCK));
    public static final Category Misc = new Category("Misc", () -> DisplayItemUtils.toStack(Items.LAVA_BUCKET));

    // Onyix Breeze-style categories
    public static final Category Legit = new Category("Legit", () -> DisplayItemUtils.toStack(Items.SHIELD));
    public static final Category HUD = new Category("HUD", () -> DisplayItemUtils.toStack(Items.COMPASS));
    public static final Category Script = new Category("Script", () -> DisplayItemUtils.toStack(Items.REPEATING_COMMAND_BLOCK));
    public static final Category Favorite = new Category("Favorite", () -> DisplayItemUtils.toStack(Items.NETHER_STAR));
    public static final Category Breeze = new Category("Breeze", () -> DisplayItemUtils.toStack(Items.WIND_CHARGE));

    public static boolean REGISTERING;

    public static void init() {
        REGISTERING = true;

        // Meteor legacy categories
        Modules.registerCategory(Combat);
        Modules.registerCategory(Player);
        Modules.registerCategory(Movement);
        Modules.registerCategory(Render);
        Modules.registerCategory(World);
        Modules.registerCategory(Misc);

        // Onyix Breeze-style categories
        Modules.registerCategory(Legit);
        Modules.registerCategory(HUD);
        Modules.registerCategory(Script);
        Modules.registerCategory(Favorite);
        Modules.registerCategory(Breeze);

        // Addons
        AddonManager.ADDONS.forEach(MeteorAddon::onRegisterCategories);

        REGISTERING = false;
    }
}
