package com.yourname.smartswapper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public class SmartSwapperClient implements ClientModInitializer {

    public static KeyMapping swapKey;
    
    private static int delayTicks = -1;
    private static int targetInventorySlot = -1;
    private static int originalHotbarSlot = -1;

    @Override
    public void onInitializeClient() {
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.smartswapper.swap",
                GLFW.GLFW_KEY_G,
                "category.smartswapper.keys"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null) return;

            while (swapKey.consumeClick()) {
                if (delayTicks == -1) {
                    startSmartSwap(client, player);
                }
            }

            if (delayTicks > 0) {
                delayTicks--;
            } else if (delayTicks == 0) {
                executeFinalEquipAndReturn(client, player);
            }
        });
    }

    private void startSmartSwap(Minecraft client, LocalPlayer player) {
        targetInventorySlot = findValidSwapItem(player);
        if (targetInventorySlot == -1) return;

        originalHotbarSlot = player.getInventory().selected;

        client.gameMode.handleInventoryMouseClick(
                player.containerMenu.containerId, 
                targetInventorySlot, 
                originalHotbarSlot, 
                ClickType.SWAP, 
                player
        );

        player.swing(InteractionHand.MAIN_HAND);
        delayTicks = ThreadLocalRandom.current().nextInt(1, 3);
    }

    private void executeFinalEquipAndReturn(Minecraft client, LocalPlayer player) {
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);

        client.gameMode.handleInventoryMouseClick(
                player.containerMenu.containerId, 
                targetInventorySlot, 
                originalHotbarSlot, 
                ClickType.SWAP, 
                player
        );

        delayTicks = -1;
        targetInventorySlot = -1;
        originalHotbarSlot = -1;
    }

    private int findValidSwapItem(LocalPlayer player) {
        ItemStack currentArmor = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearingElytra = currentArmor.getItem() instanceof ElytraItem;

        for (int i = 9; i < 45; i++) {
            ItemStack stack = player.getInventory().getItem(i < 36 ? i : i - 36);
            if (stack.isEmpty()) continue;

            if (wearingElytra && stack.getItem() instanceof ArmorItem && 
                    ((ArmorItem) stack.getItem()).getType() == ArmorItem.Type.CHESTPLATE) {
                return i;
            }
            if (!wearingElytra && stack.getItem() instanceof ElytraItem) {
                return i;
            }
        }
        return -1;
    }
}
