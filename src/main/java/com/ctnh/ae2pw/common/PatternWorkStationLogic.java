package com.ctnh.ae2pw.common;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.AESmithingTablePattern;
import appeng.crafting.pattern.AEStonecuttingPattern;
import appeng.parts.encoding.EncodingMode;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PatternWorkStationLogic implements InternalInventoryHost {

    private final IPatternWorkstationLogicHost host;

    private static final int MAX_INPUT_SLOTS = Math.max(AECraftingPattern.CRAFTING_GRID_SLOTS,
            AEProcessingPattern.MAX_INPUT_SLOTS);
    private static final int MAX_OUTPUT_SLOTS = AEProcessingPattern.MAX_OUTPUT_SLOTS;

    public static final int MAX_PATTERN_SLOTS = 144;

    /**
     * -- GETTER --
     *  The inventory used to store the inputs for encoding them into a pattern. Does not contain real items.
     *  <p/>
     *  Used for all
     * .
     */
    @Getter
    private final ConfigInventory encodedInputInv = ConfigInventory.configStacks(null, MAX_INPUT_SLOTS,
            this::onEncodedInputChanged, true);
    /**
     * -- GETTER --
     *  The inventory used to store the outputs for encoding them into a pattern. Does not contain real items.
     *  <p/>
     *  Not used for crafting
     * .
     */
    @Getter
    private final ConfigInventory encodedOutputInv = ConfigInventory.configStacks(null, MAX_OUTPUT_SLOTS,
            this::onEncodedOutputChanged, true);

    @Getter
    private final AppEngInternalInventory encodedPatternInv = new AppEngInternalInventory(this, MAX_PATTERN_SLOTS, 1, new PatternWorkStationMenu.PatternSlotFilter());

    @Getter
    private EncodingMode mode = EncodingMode.CRAFTING;
    private boolean substitute = false;
    private boolean substituteFluids = true;
    private boolean isLoading = false;
    @Nullable
    private ResourceLocation stonecuttingRecipeId;

    public PatternWorkStationLogic(IPatternWorkstationLogicHost host) {
        this.host = host;
        //this.blankPatternInv.setFilter(new AEItemDefinitionFilter(AEItems.BLANK_PATTERN));
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        // Load the encoded inputs and outputs of a pattern if it changes
//        if (inv == this.encodedPatternInv) {
//            loadEncodedPattern(encodedPatternInv.getStackInSlot(0));
//        }

        saveChanges();
    }

    @Override
    public void saveChanges() {
        // Do not re-save while we're loading since it could overwrite the NBT with incomplete data
        if (!isLoading) {
            host.markForSave();
        }
    }

    @Override
    public boolean isClientSide() {
        return host.getLevel().isClientSide();
    }

    private void onEncodedInputChanged() {
        fixCraftingRecipes();
        saveChanges();
    }

    private void onEncodedOutputChanged() {
        saveChanges();
    }

    private void loadEncodedPattern(ItemStack pattern) {
        if (pattern.isEmpty()) {
            return;
        }

        var details = PatternDetailsHelper.decodePattern(pattern, host.getLevel());

        if (details instanceof AECraftingPattern craftingPattern) {
            loadCraftingPattern(craftingPattern);
        } else if (details instanceof AEProcessingPattern processingPattern) {
            loadProcessingPattern(processingPattern);
        } else if (details instanceof AESmithingTablePattern smithingTablePattern) {
            loadSmithingTablePattern(smithingTablePattern);
        } else if (details instanceof AEStonecuttingPattern stonecuttingPattern) {
            loadStonecuttingPattern(stonecuttingPattern);
        }

        saveChanges();
    }

    private void loadCraftingPattern(AECraftingPattern pattern) {
        setMode(EncodingMode.CRAFTING);
        this.substitute = pattern.canSubstitute();
        this.substituteFluids = pattern.canSubstituteFluids();

        fillInventoryFromSparseStacks(encodedInputInv, pattern.getSparseInputs());
        fillInventoryFromSparseStacks(encodedOutputInv, pattern.getSparseOutputs());
    }

    private void loadProcessingPattern(AEProcessingPattern pattern) {
        setMode(EncodingMode.PROCESSING);

        fillInventoryFromSparseStacks(encodedInputInv, pattern.getSparseInputs());
        fillInventoryFromSparseStacks(encodedOutputInv, pattern.getSparseOutputs());
    }

    private void loadSmithingTablePattern(AESmithingTablePattern pattern) {
        setMode(EncodingMode.SMITHING_TABLE);
        this.substitute = pattern.canSubstitute();

        encodedInputInv.clear();
        encodedInputInv.setStack(0, new GenericStack(pattern.getTemplate(), 1));
        encodedInputInv.setStack(1, new GenericStack(pattern.getBase(), 1));
        encodedInputInv.setStack(2, new GenericStack(pattern.getAddition(), 1));
        encodedOutputInv.clear();
    }

    private void loadStonecuttingPattern(AEStonecuttingPattern pattern) {
        setMode(EncodingMode.STONECUTTING);
        stonecuttingRecipeId = pattern.getRecipeId();

        this.substitute = pattern.canSubstitute;

        encodedInputInv.clear();
        encodedInputInv.setStack(0, new GenericStack(pattern.getInput(), 1));
        encodedOutputInv.clear();
    }

    private static void fillInventoryFromSparseStacks(ConfigInventory inv, GenericStack[] stacks) {
        inv.beginBatch();
        try {
            for (int i = 0; i < inv.size(); i++) {
                inv.setStack(i, i < stacks.length ? stacks[i] : null);
            }
        } finally {
            inv.endBatch();
        }
    }

    public void setMode(EncodingMode mode) {
        this.mode = mode;
        this.fixCraftingRecipes();
        this.saveChanges();
    }

    public boolean isSubstitution() {
        return this.substitute;
    }

    public void setSubstitution(boolean canSubstitute) {
        this.substitute = canSubstitute;
        this.saveChanges();
    }

    public boolean isFluidSubstitution() {
        return this.substituteFluids;
    }

    public void setFluidSubstitution(boolean canSubstitute) {
        this.substituteFluids = canSubstitute;
        this.saveChanges();
    }

    public @Nullable ResourceLocation getStonecuttingRecipeId() {
        return stonecuttingRecipeId;
    }

    public void setStonecuttingRecipeId(ResourceLocation stonecuttingRecipeId) {
        this.stonecuttingRecipeId = stonecuttingRecipeId;
        this.saveChanges();
    }

    /**
     * Inventory of size 1, which contains the blank patterns for encoding.
     */
//    public InternalInventory getBlankPatternInv() {
//        return blankPatternInv;
//    }

    public void readFromNBT(CompoundTag data) {
        isLoading = true;
        try {
            try {
                this.mode = EncodingMode.valueOf(data.getString("mode"));
            } catch (IllegalArgumentException ignored) {
                this.mode = EncodingMode.CRAFTING;
            }
            this.setSubstitution(data.getBoolean("substitute"));
            this.setFluidSubstitution(data.getBoolean("substituteFluids"));

            if (data.contains("stonecuttingRecipeId", Tag.TAG_STRING)) {
                this.stonecuttingRecipeId = ResourceLocation.tryParse(data.getString("stonecuttingRecipeId"));
            } else {
                this.stonecuttingRecipeId = null;
            }

            //blankPatternInv.readFromNBT(data, "blankPattern");
            encodedPatternInv.readFromNBT(data, "encodedPattern");

            encodedInputInv.readFromChildTag(data, "encodedInputs");
            encodedOutputInv.readFromChildTag(data, "encodedOutputs");
        } finally {
            isLoading = false;
        }
    }

    public void writeToNBT(CompoundTag data) {
        data.putString("mode", this.mode.name());
        data.putBoolean("substitute", this.substitute);
        data.putBoolean("substituteFluids", this.substituteFluids);
        if (this.stonecuttingRecipeId != null) {
            data.putString("stonecuttingRecipeId", this.stonecuttingRecipeId.toString());
        }
        //blankPatternInv.writeToNBT(data, "blankPattern");
        encodedPatternInv.writeToNBT(data, "encodedPattern");
        encodedInputInv.writeToChildTag(data, "encodedInputs");
        encodedOutputInv.writeToChildTag(data, "encodedOutputs");
    }

    private void fixCraftingRecipes() {
        // Do not do this on the client since the server will always sync the correct state to us anyway
        if (host.getLevel() == null || host.getLevel().isClientSide()) {
            return;
        }

        if (getMode() != EncodingMode.PROCESSING) {
            var craftingGrid = getEncodedInputInv();
            for (int slot = 0; slot < craftingGrid.size(); slot++) {
                var stack = craftingGrid.getStack(slot);
                if (stack == null) {
                    continue;
                }

                // Crafting recipes only support real items, not wrapped fluids or similar things
                if (!AEItemKey.is(stack.what())) {
                    craftingGrid.setStack(slot, null);
                    continue;
                }

                // Clamp item count to 1 for crafting recipes
                if (stack.amount() != 1) {
                    craftingGrid.setStack(slot, new GenericStack(stack.what(), 1));
                }
            }
        }
    }
}
