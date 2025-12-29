package com.ctnh.ae2pw.utils;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;

public class PatternBufferSlot extends RestrictedInputSlot {
    public PatternBufferSlot(InternalInventory inv, int invSlot) {
        super(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, inv, invSlot);
        setStackLimit(1);
    }
}
