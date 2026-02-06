package com.ctnh.ae2pw.utils;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenCustomHashMap;
import net.minecraft.world.item.ItemStack;

public class StackMap extends Object2BooleanOpenCustomHashMap<ItemStack> {
    public StackMap() {
        super(new Hash.Strategy<>() {
            @Override
            public int hashCode(ItemStack o) {
                return o.getItem().hashCode() ^ (o.hasTag() ? o.getTag().hashCode() : 0xFFFFFFFF);
            }

            @Override
            public boolean equals(ItemStack a, ItemStack b) {
                return a == b || (a != null && b != null && ItemStack.isSameItemSameTags(a, b));
            }
        });
    }


}
