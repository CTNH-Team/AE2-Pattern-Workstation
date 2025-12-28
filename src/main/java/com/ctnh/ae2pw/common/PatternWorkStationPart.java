/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package com.ctnh.ae2pw.common;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;

import appeng.parts.reporting.AbstractTerminalPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

import static appeng.parts.encoding.PatternEncodingTerminalPart.MODEL_OFF;
import static appeng.parts.encoding.PatternEncodingTerminalPart.MODEL_ON;


public class PatternWorkStationPart extends AbstractTerminalPart
        implements IPatternWorkstationLogicHost, IPatternWorkStationMenuHost {

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final PatternWorkStationLogic logic = new PatternWorkStationLogic(this);

    public PatternWorkStationPart(IPartItem<?> partItem) {
        super(partItem);
        //getConfigManager().registerSetting(Settings.TERMINAL_SHOW_PATTERN_PROVIDERS, ShowPatternProviders.VISIBLE);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
//        for (var is : this.logic.getBlankPatternInv()) {
//            drops.add(is);
//        }
        for (var is : this.logic.getEncodedPatternInv()) {
            drops.add(is);
        }
        for (var is : this.logic.getPatternRecycleInv()) {
            drops.add(is);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        //this.logic.getBlankPatternInv().clear();
        this.logic.getEncodedPatternInv().clear();
        this.logic.getPatternRecycleInv().clear();
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        logic.readFromNBT(data);
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        logic.writeToNBT(data);
    }

    @Override
    public MenuType<?> getMenuType(Player p) {
        return PatternWorkStationMenu.TYPE;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public PatternWorkStationLogic getLogic() {
        return logic;
    }

    @Override
    public void markForSave() {
        getHost().markForSave();
    }

//    @Override
//    public <T> LazyOptional<T> getCapability(Capability<T> cap) {
//        if (cap == ForgeCapabilities.ITEM_HANDLER) {
//            return LazyOptional.of(() -> logic.getBlankPatternInv().toItemHandler()).cast();
//        }
//        return super.getCapability(cap);
//    }
}
