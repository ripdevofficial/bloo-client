package com.blood;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public class Modules {
    public static List<Module> ALL;

    /** Closest other player within maxDist blocks, or null. */
    static Player nearest(Minecraft mc, double maxDist) {
        Player best = null;
        double bestDist = maxDist * maxDist;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive()) continue;
            double d = mc.player.distanceToSqr(p);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }


    /** Inventory-menu slot number for inventory index i (0-8 hotbar, 9-35 main). */
    private static int menuSlot(int i) { return i < 9 ? 36 + i : i; }

    private static int findInInventory(Player me, boolean elytra) {
        for (int i = 0; i < 36; i++) {
            ItemStack st = me.getInventory().getItem(i);
            if (elytra ? st.is(Items.ELYTRA) : (st.is(ItemTags.CHEST_ARMOR) && !st.is(Items.ELYTRA))) return i;
        }
        return -1;
    }

    /** Takes the elytra off (swapping in a chestplate if you carry one). Returns false if you aren't wearing an elytra. */
    static boolean elytraOff(Minecraft mc) {
        Player me = mc.player;
        if (!me.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return false;
        int cid = me.inventoryMenu.containerId;
        int plate = findInInventory(me, false);
        if (plate >= 0) {
            int slot = menuSlot(plate);
            mc.gameMode.handleInventoryMouseClick(cid, slot, 0, ClickType.PICKUP, me);
            mc.gameMode.handleInventoryMouseClick(cid, 6, 0, ClickType.PICKUP, me);
            mc.gameMode.handleInventoryMouseClick(cid, slot, 0, ClickType.PICKUP, me);
        } else {
            mc.gameMode.handleInventoryMouseClick(cid, 6, 0, ClickType.QUICK_MOVE, me); // just unequip it
        }
        return true;
    }

    /** Puts the elytra back on from your inventory. */
    static void elytraOn(Minecraft mc) {
        Player me = mc.player;
        if (me.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        int el = findInInventory(me, true);
        if (el < 0) return;
        int cid = me.inventoryMenu.containerId;
        int slot = menuSlot(el);
        if (me.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(cid, slot, 0, ClickType.QUICK_MOVE, me);
        } else {
            mc.gameMode.handleInventoryMouseClick(cid, slot, 0, ClickType.PICKUP, me);
            mc.gameMode.handleInventoryMouseClick(cid, 6, 0, ClickType.PICKUP, me);
            mc.gameMode.handleInventoryMouseClick(cid, slot, 0, ClickType.PICKUP, me);
        }
    }

    public static void init() {
        ALL = List.of(
            new Module("Fly") {
                @Override public void onTick(Minecraft mc) {
                    var a = mc.player.getAbilities();
                    a.mayfly = true;
                    a.setFlyingSpeed(0.15F); // vanilla is 0.05
                }
                @Override public void onDisable(Minecraft mc) {
                    if (mc.player == null) return;
                    var a = mc.player.getAbilities();
                    a.setFlyingSpeed(0.05F);
                    if (!mc.player.isCreative() && !mc.player.isSpectator()) { a.mayfly = false; a.flying = false; }
                }
            },

            new Module("Sprint") {
                @Override public void onTick(Minecraft mc) {
                    if (mc.options.keyUp.isDown() && !mc.player.isShiftKeyDown()) mc.player.setSprinting(true);
                }
            },

            new Module("ElytraFly") {
                @Override public void onTick(Minecraft mc) {
                    if (!mc.player.isFallFlying() || !mc.options.keyUp.isDown()) return;
                    Vec3 look = mc.player.getLookAngle();
                    mc.player.setDeltaMovement(look.scale(1.8)); // flies where you look while holding W
                }
            },

            new Module("PlayerESP") {
                @Override public void onTick(Minecraft mc) {
                    for (Player p : mc.level.players()) if (p != mc.player) p.setGlowingTag(true);
                }
                @Override public void onDisable(Minecraft mc) {
                    if (mc.level == null) return;
                    for (Player p : mc.level.players()) p.setGlowingTag(false);
                }
            },

            new Module("Fullbright") {
                @Override public void onTick(Minecraft mc) {
                    if (!mc.player.hasEffect(MobEffects.NIGHT_VISION))
                        mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000000, 0, false, false));
                }
                @Override public void onDisable(Minecraft mc) {
                    if (mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
                }
            },

            new Module("ElytraMace") {
                private boolean elytraRemoved;

                @Override public void onDisable(Minecraft mc) {
                    if (elytraRemoved && mc.player != null) elytraOn(mc);
                    elytraRemoved = false;
                }

                @Override public void onTick(Minecraft mc) {
                    Player me = mc.player;
                    Player target = nearest(mc, 40);

                    // PHASE 2: elytra is off, we are falling onto the target
                    if (elytraRemoved) {
                        if (target != null) {
                            Vec3 d = target.position().subtract(me.position());
                            Vec3 h = new Vec3(d.x, 0, d.z);
                            if (h.lengthSqr() > 0.01) {
                                h = h.normalize().scale(0.7);
                                me.setDeltaMovement(h.x, me.getDeltaMovement().y, h.z); // steer toward target
                            }
                            me.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                            if (me.getMainHandItem().is(Items.MACE) && me.distanceToSqr(target) <= 12.25
                                    && me.fallDistance > 1.5 && me.getAttackStrengthScale(0F) >= 1F) {
                                mc.gameMode.attack(me, target); // smash!
                                me.swing(InteractionHand.MAIN_HAND);
                                elytraOn(mc);
                                elytraRemoved = false;
                                return;
                            }
                        }
                        if (me.onGround() || target == null) { elytraOn(mc); elytraRemoved = false; }
                        return;
                    }

                    // PHASE 1: gliding. Grab the mace, aim at the nearest player and dive
                    if (!me.isFallFlying()) return;
                    if (!me.getMainHandItem().is(Items.MACE)) {
                        for (int i = 0; i < 9; i++) {
                            if (me.getInventory().getItem(i).is(Items.MACE)) { me.getInventory().setSelectedSlot(i); break; }
                        }
                    }
                    if (target == null) return;
                    me.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                    me.setDeltaMovement(me.getLookAngle().scale(2.0));

                    // when we are above the target and close enough, drop the elytra so fall distance can build up
                    boolean above = me.getY() - target.getY() >= 6;
                    if (above && me.distanceToSqr(target) <= 18 * 18 && me.getMainHandItem().is(Items.MACE)) {
                        if (elytraOff(mc)) elytraRemoved = true;
                    }
                }
            },

            new Module("KillAura") {
                @Override public void onTick(Minecraft mc) {
                    if (mc.player.getAttackStrengthScale(0F) < 1F) return; // wait for full cooldown
                    Player best = null;
                    double bestDist = 16.0; // 4 blocks, squared
                    for (Player p : mc.level.players()) {
                        if (p == mc.player || !p.isAlive()) continue;
                        double d = mc.player.distanceToSqr(p);
                        if (d < bestDist) { bestDist = d; best = p; }
                    }
                    if (best != null) {
                        mc.gameMode.attack(mc.player, best);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            },

            new Module("StashFinder") {
                private final Set<Long> reported = new HashSet<>();
                private int ticks;
                @Override public void onEnable(Minecraft mc) { reported.clear(); }
                @Override public void onTick(Minecraft mc) {
                    if (++ticks % 40 != 0) return; // scan every 2 seconds
                    int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
                    for (int dx = -8; dx <= 8; dx++) {
                        for (int dz = -8; dz <= 8; dz++) {
                            int cx = pcx + dx, cz = pcz + dz;
                            LevelChunk chunk = mc.level.getChunk(cx, cz);
                            int storage = 0;
                            for (BlockEntity be : chunk.getBlockEntities().values()) {
                                if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity) storage++;
                            }
                            if (storage >= 5 && reported.add(ChunkPos.asLong(cx, cz))) {
                                mc.player.displayClientMessage(Component.literal(
                                    "[Blood] Possible stash: " + storage + " storage blocks near X " + (cx * 16) + " Z " + (cz * 16)), false);
                            }
                        }
                    }
                }
            }
        );
    }
}
