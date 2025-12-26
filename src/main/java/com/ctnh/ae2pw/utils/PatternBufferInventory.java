package com.ctnh.ae2pw.utils;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.core.definitions.AEItems;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PatternBufferInventory extends AppEngInternalInventory {
    public PatternBufferInventory(@Nullable InternalInventoryHost inventory, int size) {
        super(inventory, size, 1, new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return !stack.isEmpty() && PatternDetailsHelper.isEncodedPattern(stack);
            }
        });


    }

    public boolean quickInsert(ItemStack stack) {
        if (stack.isEmpty() && isItemValid(0, stack)) {
            return false;
        }

        for (int i = 0; i < size(); i++) {
            if (getStackInSlot(i).isEmpty()) {
                ItemStack toInsert = stack.copy();
                toInsert.setCount(1);
                setItemDirect(i, toInsert);
                return true;
            }
        }

        return false;
    }

    public ItemStack quickExtract() {
        for (int i = size() - 1; i >= 0; i--) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {
                setItemDirect(i, ItemStack.EMPTY);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public void compact() {
        int targetIndex = 0;

        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStackInSlot(i);

            if (!stack.isEmpty()) {
                if (i != targetIndex) {
                    setItemDirect(targetIndex, stack);
                    setItemDirect(i, ItemStack.EMPTY);
                }
                targetIndex++;
            }
        }
    }
}
