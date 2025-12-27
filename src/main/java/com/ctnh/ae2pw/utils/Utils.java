package com.ctnh.ae2pw.utils;

import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.item.ItemStack;

public class Utils {
    public static boolean quickInsert(InternalInventory inventory, ItemStack stack) {
        if (stack.isEmpty() || !inventory.isItemValid(0, stack)) {
            return false;
        }

        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                ItemStack toInsert = stack.copy();
                toInsert.setCount(1);
                inventory.setItemDirect(i, toInsert);
                return true;
            }
        }

        return false;
    }

    public static ItemStack quickExtract(InternalInventory inventory) {
        for (int i = inventory.size() - 1; i >= 0; i--) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inventory.setItemDirect(i, ItemStack.EMPTY);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean quickTransfer(
            InternalInventory from,
            InternalInventory to
    ) {
        // 从后往前找可提取的物品
        for (int i = from.size() - 1; i >= 0; i--) {
            ItemStack stack = from.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            // 校验目标库存是否接受该物品
            if (!to.isItemValid(0, stack)) {
                continue;
            }

            // 尝试插入目标库存
            for (int j = 0; j < to.size(); j++) {
                if (to.getStackInSlot(j).isEmpty()) {
                    // 插入 1 个
                    ItemStack one = stack.copy();
                    one.setCount(1);
                    to.setItemDirect(j, one);

                    // 源库存减 1
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        from.setItemDirect(i, ItemStack.EMPTY);
                    }

                    return true;
                }
            }
        }

        return false;
    }


}
