package com.ctnh.ae2pw.utils;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.menu.slot.RestrictedInputSlot;

public class PatternRecycleSlot extends RestrictedInputSlot {
    IGrid grid;
    IActionSource actionSource;
    public PatternRecycleSlot(InternalInventory inv, int invSlot, IGrid grid, IActionSource actionSource) {
        super(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, inv, invSlot);
        setStackLimit(1);
    }

//    @Override
//    public ItemStack safeInsert(ItemStack stack) {
//        if(mayPlace(stack) && !getItem().isEmpty()){
//            if(grid == null || actionSource == null) return stack;
//            set(ItemStack.EMPTY);
//            grid.getStorageService().getInventory().insert(
//                    AEItemKey.of(AEItems.BLANK_PATTERN),
//                    1,
//                    Actionable.MODULATE,
//                    actionSource
//            );
//        }
//        return super.safeInsert(stack);
//    }
}
