package com.ctnh.ae2pw.common;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.ShowPatternProviders;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.Icon;
import appeng.core.AELog;
import appeng.core.definitions.AEItems;
import appeng.core.sync.packets.ClearPatternAccessTerminalPacket;
import appeng.core.sync.packets.PatternAccessTerminalPacket;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.IMenuCraftingPacket;
import appeng.helpers.InventoryAction;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.PatternTermSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.AEBasePart;
import appeng.parts.encoding.EncodingMode;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.ctnh.ae2pw.utils.PatternBufferSlot;
import com.ctnh.ae2pw.utils.PatternRecycleSlot;
import com.ctnh.ae2pw.utils.Utils;
import com.glodblock.github.extendedae.network.EPPNetworkHandler;
import com.glodblock.github.extendedae.network.packet.SExPatternInfo;
import com.glodblock.github.extendedae.xmod.LoadList;
import com.glodblock.github.extendedae.xmod.gregtech.MetaTileResolver;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.ctnh.ae2pw.common.PatternWorkStationLogic.MAX_PATTERN_SLOTS;

public class PatternWorkStationMenu extends MEStorageMenu implements IMenuCraftingPacket {
    public static final MenuType<PatternWorkStationMenu> TYPE = MenuTypeBuilder
            .create(PatternWorkStationMenu::new, IPatternWorkStationMenuHost.class)
            .build("patternworkstation");

    //////////////////////
    ////Pattern Encode////
    //////////////////////
    private static final int CRAFTING_GRID_WIDTH = 3;
    private static final int CRAFTING_GRID_HEIGHT = 3;
    private static final int CRAFTING_GRID_SLOTS = CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT;

    private static final String ACTION_SET_MODE = "setMode";
    private static final String ACTION_ENCODE = "encode";
    private static final String ACTION_CLEAR = "clear";
    private static final String ACTION_SET_SUBSTITUTION = "setSubstitution";
    private static final String ACTION_SET_FLUID_SUBSTITUTION = "setFluidSubstitution";
    private static final String ACTION_SET_STONECUTTING_RECIPE_ID = "setStonecuttingRecipeId";
    private static final String ACTION_CYCLE_PROCESSING_OUTPUT = "cycleProcessingOutput";

    private static final String ACTION_QUICK_MOVE_PATTERN = "quickMovePattern";
    private static final String ACTION_TP_TO_PROVIDER = "tpToProvider";

    private final PatternWorkStationLogic encodingLogic;
    @Getter
    private final FakeSlot[] craftingGridSlots = new FakeSlot[9];
    @Getter
    private final FakeSlot[] processingInputSlots = new FakeSlot[AEProcessingPattern.MAX_INPUT_SLOTS];
    @Getter
    private final FakeSlot[] processingOutputSlots = new FakeSlot[AEProcessingPattern.MAX_OUTPUT_SLOTS];
    private final FakeSlot stonecuttingInputSlot;
    @Getter
    private final FakeSlot smithingTableTemplateSlot;
    @Getter
    private final FakeSlot smithingTableBaseSlot;
    @Getter
    private final FakeSlot smithingTableAdditionSlot;
    private final PatternTermSlot craftOutputSlot;
    //private final RestrictedInputSlot blankPatternSlot;
    public final RestrictedInputSlot[] encodedPatternSlots;
    public final RestrictedInputSlot patternRecycleSlot;
    // 9x9 inventory wrapper to feed into the crafting mode slots

    private final ConfigInventory encodedInputsInv;
    private final ConfigInventory encodedOutputsInv;

    private CraftingRecipe currentRecipe;
    // The current mode is essentially the last-known client-side version of mode
    private EncodingMode currentMode;

    @Getter
    @GuiSync(97)
    public EncodingMode mode = EncodingMode.CRAFTING;
    @Getter
    @GuiSync(96)
    public boolean substitute = false;
    @Getter
    @GuiSync(95)
    public boolean substituteFluids = true;
    @GuiSync(94)
    @Nullable
    public ResourceLocation stonecuttingRecipeId;

    @GuiSync(93)
    public int selectedPatternSlot = -1;

    @Getter
    private final List<StonecutterRecipe> stonecuttingRecipes = new ArrayList<>();

    /**
     * Whether fluids can be substituted or not depends on the recipe. This set contains the slots of the crafting
     * matrix that support such substitution.
     */
    public IntSet slotsSupportingFluidSubstitution = new IntArraySet();

    //////////////////////
    ////Pattern Access////
    //////////////////////

    @GuiSync(5)
    @Getter
    public ShowPatternProviders shownPatternProviders = ShowPatternProviders.VISIBLE;

    private static long inventorySerial = 1; //0 represents ME storage slot, we start from 1
    private final Map<PatternContainer, ContainerTracker> diList = new IdentityHashMap<>();
    private final Long2ObjectOpenHashMap<ContainerTracker> byId = new Long2ObjectOpenHashMap<>();
    /**
     * Tracks hosts that were visible before, even if they no longer match the filter. For
     * {@link ShowPatternProviders#NOT_FULL}.
     */
    private final Set<PatternContainer> pinnedHosts = Collections.newSetFromMap(new IdentityHashMap<>());

    public String patternSearch = "";

    public PatternWorkStationMenu(int id, Inventory ip, IPatternWorkStationMenuHost host) {
        this(TYPE, id, ip, host, true);
    }

    public PatternWorkStationMenu(MenuType<?> menuType, int id, Inventory ip, IPatternWorkStationMenuHost host, boolean bindInventory) {
        super(menuType, id, ip, host, bindInventory);

        this.encodingLogic = host.getLogic();
        this.encodedInputsInv = encodingLogic.getEncodedInputInv();
        this.encodedOutputsInv = encodingLogic.getEncodedOutputInv();

        // Wrappers for use with slots
        var encodedInputs = encodedInputsInv.createMenuWrapper();
        var encodedOutputs = encodedOutputsInv.createMenuWrapper();

        // Create the 3x3 crafting input grid for crafting mode
        for (int i = 0; i < CRAFTING_GRID_SLOTS; i++) {
            var slot = new FakeSlot(encodedInputs, i);
            slot.setHideAmount(true);
            this.addSlot(this.craftingGridSlots[i] = slot, SlotSemantics.CRAFTING_GRID);
        }
        // Create the output slot used for crafting mode patterns
        this.addSlot(this.craftOutputSlot = new PatternTermSlot(ip.player, this.getActionSource(), this.powerSource,
                        host.getInventory(), encodedInputs, this),
                SlotSemantics.CRAFTING_RESULT);
        this.craftOutputSlot.setIcon(null);

        // Create as many slots as needed for processing inputs and outputs
        for (int i = 0; i < processingInputSlots.length; i++) {
            this.addSlot(this.processingInputSlots[i] = new FakeSlot(encodedInputs, i),
                    SlotSemantics.PROCESSING_INPUTS);
        }
        for (int i = 0; i < this.processingOutputSlots.length; i++) {
            this.addSlot(this.processingOutputSlots[i] = new FakeSlot(encodedOutputs, i),
                    SlotSemantics.PROCESSING_OUTPUTS);
        }
        this.processingOutputSlots[0].setIcon(Icon.BACKGROUND_PRIMARY_OUTPUT);

        // Input for stonecutting pattern encoding
        this.addSlot(this.stonecuttingInputSlot = new FakeSlot(encodedInputs, 0),
                SlotSemantics.STONECUTTING_INPUT);
        this.stonecuttingInputSlot.setHideAmount(true);

        // Input for smithing table pattern encoding
        this.addSlot(this.smithingTableTemplateSlot = new FakeSlot(encodedInputs, 0),
                SlotSemantics.SMITHING_TABLE_TEMPLATE);
        this.smithingTableTemplateSlot.setHideAmount(true);
        this.addSlot(this.smithingTableBaseSlot = new FakeSlot(encodedInputs, 1),
                SlotSemantics.SMITHING_TABLE_BASE);
        this.smithingTableBaseSlot.setHideAmount(true);
        this.addSlot(this.smithingTableAdditionSlot = new FakeSlot(encodedInputs, 2),
                SlotSemantics.SMITHING_TABLE_ADDITION);
        this.smithingTableAdditionSlot.setHideAmount(true);

        this.encodedPatternSlots = new PatternBufferSlot[MAX_PATTERN_SLOTS];
        for(int i=0; i<MAX_PATTERN_SLOTS; i++){
            this.addSlot(
                    encodedPatternSlots[i] = new PatternBufferSlot(encodingLogic.getEncodedPatternInv(), i),
                    SlotSemantics.ENCODED_PATTERN);
        }

        patternRecycleSlot = new PatternRecycleSlot(encodingLogic.getPatternRecycleInv(), 0, getGrid(), getActionSource());

        this.addSlot(patternRecycleSlot, SlotSemantics.BLANK_PATTERN); //do not bother registering new SlotSemantics


        registerClientAction(ACTION_ENCODE, Long.class, this::encode);
        registerClientAction(ACTION_SET_STONECUTTING_RECIPE_ID, ResourceLocation.class,
                encodingLogic::setStonecuttingRecipeId);
        registerClientAction(ACTION_CLEAR, this::clear);
        registerClientAction(ACTION_SET_MODE, EncodingMode.class, encodingLogic::setMode);
        registerClientAction(ACTION_SET_SUBSTITUTION, Boolean.class, encodingLogic::setSubstitution);
        registerClientAction(ACTION_SET_FLUID_SUBSTITUTION, Boolean.class, encodingLogic::setFluidSubstitution);
        registerClientAction(ACTION_CYCLE_PROCESSING_OUTPUT, this::cycleProcessingOutput);
        registerClientAction(ACTION_TP_TO_PROVIDER, Long.class, this::tpToProvider);

        updateStonecuttingRecipes();
    }

    @Override
    public void setItem(int slotID, int stateId, ItemStack stack) {
        super.setItem(slotID, stateId, stack);
        this.getAndUpdateOutput();
    }

    private ItemStack getAndUpdateOutput() {
        var level = this.getPlayerInventory().player.level();
        var ic = new TransientCraftingContainer(this, CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT);

        boolean invalidIngredients = false;
        for (int x = 0; x < ic.getContainerSize(); x++) {
            var stack = getEncodedCraftingIngredient(x);
            if (stack != null) {
                ic.setItem(x, stack);
            } else {
                invalidIngredients = true;
            }
        }

        if (this.currentRecipe == null || !this.currentRecipe.matches(ic, level)) {
            if (invalidIngredients) {
                this.currentRecipe = null;
            } else {
                this.currentRecipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, ic, level).orElse(null);
            }
            this.currentMode = this.mode;
            checkFluidSubstitutionSupport();
        }

        final ItemStack is;

        if (this.currentRecipe == null) {
            is = ItemStack.EMPTY;
        } else {
            is = this.currentRecipe.assemble(ic, level.registryAccess());
        }

        this.craftOutputSlot.setDisplayedCraftingOutput(is);
        return is;
    }

    private void checkFluidSubstitutionSupport() {
        this.slotsSupportingFluidSubstitution.clear();

        if (this.currentRecipe == null) {
            return; // No recipe -> no substitution
        }

        var encodedPattern = encodePattern();
        if (encodedPattern != null) {
            var decodedPattern = PatternDetailsHelper.decodePattern(encodedPattern,
                    this.getPlayerInventory().player.level());
            if (decodedPattern instanceof AECraftingPattern craftingPattern) {
                for (int i = 0; i < craftingPattern.getSparseInputs().length; i++) {
                    if (craftingPattern.getValidFluid(i) != null) {
                        slotsSupportingFluidSubstitution.add(i);
                    }
                }
            }
        }
    }

    public void encode(Long targetId) {
        if (isClientSide()) {
            sendClientAction(ACTION_ENCODE, targetId);
            return;
        }

        ItemStack encodedPattern = encodePattern();
        if (encodedPattern != null && getGrid() != null) {
            var blankPattern = getGrid().getStorageService().getInventory().extract(
                    AEItemKey.of(AEItems.BLANK_PATTERN),
                    1,
                    Actionable.MODULATE,
                    getActionSource()
            );
            if (blankPattern < 1) {
                return; // no blanks.
            }

            if(targetId != 0 && Utils.quickInsert(byId.get(targetId.longValue()).server, encodePattern())){
                return;
            }

            if (selectedPatternSlot == -1) {
                //var blankPattern = this.blankPatternSlot.getItem();
                Utils.quickInsert(encodingLogic.getEncodedPatternInv(), encodedPattern);
            } else {
                if(getSlot(selectedPatternSlot) instanceof PatternBufferSlot slot){
                    slot.set(encodedPattern);
                }
            }

        }
    }

    @Nullable
    private ItemStack encodePattern() {
        return switch (this.mode) {
            case CRAFTING -> encodeCraftingPattern();
            case PROCESSING -> encodeProcessingPattern();
            case SMITHING_TABLE -> encodeSmithingTablePattern();
            case STONECUTTING -> encodeStonecuttingPattern();
        };
    }

    @Nullable
    private ItemStack encodeCraftingPattern() {
        var ingredients = new ItemStack[CRAFTING_GRID_SLOTS];
        boolean valid = false;
        for (int x = 0; x < ingredients.length; x++) {
            ingredients[x] = getEncodedCraftingIngredient(x);
            if (ingredients[x] == null) {
                return null; // Invalid item
            } else if (!ingredients[x].isEmpty()) {
                // At least one input must be set, but it doesn't matter which one
                valid = true;
            }
        }
        if (!valid) {
            return null;
        }

        var result = this.getAndUpdateOutput();
        if (result.isEmpty() || currentRecipe == null) {
            return null;
        }

        return PatternDetailsHelper.encodeCraftingPattern(this.currentRecipe, ingredients, result, isSubstitute(),
                isSubstituteFluids());
    }

    @Nullable
    private ItemStack encodeProcessingPattern() {
        var inputs = new GenericStack[encodedInputsInv.size()];
        boolean valid = false;
        for (int slot = 0; slot < encodedInputsInv.size(); slot++) {
            inputs[slot] = encodedInputsInv.getStack(slot);
            if (inputs[slot] != null) {
                // At least one input must be set, but it doesn't matter which one
                valid = true;
            }
        }
        if (!valid) {
            return null;
        }

        var outputs = new GenericStack[encodedOutputsInv.size()];
        for (int slot = 0; slot < encodedOutputsInv.size(); slot++) {
            outputs[slot] = encodedOutputsInv.getStack(slot);
        }
        if (outputs[0] == null) {
            // The first output slot is required
            return null;
        }

        return PatternDetailsHelper.encodeProcessingPattern(inputs, outputs);
    }

    @Nullable
    private ItemStack encodeSmithingTablePattern() {
        if (!(encodedInputsInv.getKey(0) instanceof AEItemKey template)
                || !(encodedInputsInv.getKey(1) instanceof AEItemKey base)
                || !(encodedInputsInv.getKey(2) instanceof AEItemKey addition)) {
            return null;
        }

        var container = new SimpleContainer(3);
        container.setItem(0, template.toStack());
        container.setItem(1, base.toStack());
        container.setItem(2, addition.toStack());

        var level = getPlayer().level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMITHING, container, level)
                .orElse(null);
        if (recipe == null) {
            return null;
        }

        var output = AEItemKey.of(recipe.assemble(container, level.registryAccess()));

        return PatternDetailsHelper.encodeSmithingTablePattern(recipe, template, base, addition, output,
                encodingLogic.isSubstitution());
    }

    @Nullable
    private ItemStack encodeStonecuttingPattern() {
        // Find the selected recipe
        if (stonecuttingRecipeId == null) {
            return null;
        }

        if (!(encodedInputsInv.getKey(0) instanceof AEItemKey input)) {
            return null;
        }

        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, input.toStack());

        var level = getPlayer().level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.STONECUTTING, container, level, stonecuttingRecipeId)
                .map(Pair::getSecond)
                .orElse(null);
        if (recipe == null) {
            return null;
        }

        var output = AEItemKey.of(recipe.getResultItem(level.registryAccess()));

        return PatternDetailsHelper.encodeStonecuttingPattern(recipe, input, output, encodingLogic.isSubstitution());
    }

    /**
     * Get potential crafting ingredient encoded in given slot, return null if something is encoded in the slot, but
     * it's not an item.
     */
    @Nullable
    private ItemStack getEncodedCraftingIngredient(int slot) {
        var what = encodedInputsInv.getKey(slot);
        if (what == null) {
            return ItemStack.EMPTY;
        } else if (what instanceof AEItemKey itemKey) {
            return itemKey.toStack(1);
        } else {
            return null; // There's something in this slot that's not an item
        }
    }

    private boolean isPattern(ItemStack output) {
        if (output.isEmpty()) {
            return false;
        }

        return AEItems.BLANK_PATTERN.isSameAs(output);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {

            shownPatternProviders = getHost().getConfigManager().getSetting(Settings.TERMINAL_SHOW_PATTERN_PROVIDERS);

            super.broadcastChanges();
            //Pattern Encode
            if (this.mode != encodingLogic.getMode()) {
                this.setMode(encodingLogic.getMode());
            }

            this.substitute = encodingLogic.isSubstitution();
            this.substituteFluids = encodingLogic.isFluidSubstitution();
            this.stonecuttingRecipeId = encodingLogic.getStonecuttingRecipeId();

            if (getShownPatternProviders() != ShowPatternProviders.NOT_FULL) {
                this.pinnedHosts.clear();
            }

            IGrid grid = getGrid();

            var state = new VisitorState();
            if (grid != null) {
                for (var machineClass : grid.getMachineClasses()) {
                    if (PatternContainer.class.isAssignableFrom(machineClass)) {
                        visitPatternProviderHosts(grid, (Class<? extends PatternContainer>) machineClass, state);
                    }
                }

                // Ensure we don't keep references to removed hosts
                pinnedHosts.removeIf(host -> host.getGrid() != grid);
            } else {
                pinnedHosts.clear();
            }

            if (state.total != this.diList.size() || state.forceFullUpdate) {
                sendFullUpdate(grid);
            } else {
                sendIncrementalUpdate();
            }

        }
    }

    @Override
    public void onServerDataSync() {
        super.onServerDataSync();

        // Update slot visibility
        for (var slot : craftingGridSlots) {
            slot.setActive(mode == EncodingMode.CRAFTING);
        }
        craftOutputSlot.setActive(mode == EncodingMode.CRAFTING);
        for (var slot : processingInputSlots) {
            slot.setActive(mode == EncodingMode.PROCESSING);
        }
        for (var slot : processingOutputSlots) {
            slot.setActive(mode == EncodingMode.PROCESSING);
        }

        if (this.currentMode != this.mode) {
            this.encodingLogic.setMode(this.mode);
            this.getAndUpdateOutput();
            this.updateStonecuttingRecipes();
        }
    }

    @Override
    public void onSlotChange(Slot s) {
        if (s instanceof PatternBufferSlot && isServerSide()) {
            if(selectedPatternSlot != -1 &&
                    (s != getSlot(selectedPatternSlot) || s.getItem().isEmpty())){
                selectedPatternSlot = -1;
            }
            this.broadcastChanges();
        }

        if (s == this.craftOutputSlot && isClientSide()) {
            this.getAndUpdateOutput();
        }

        if (s == this.stonecuttingInputSlot) {
            updateStonecuttingRecipes();
        }
    }

    private void updateStonecuttingRecipes() {
        stonecuttingRecipes.clear();
        if (encodedInputsInv.getKey(0) instanceof AEItemKey itemKey) {
            var level = getPlayer().level();
            var recipeManager = level.getRecipeManager();
            var inventory = new SimpleContainer(1);
            inventory.setItem(0, itemKey.toStack());
            stonecuttingRecipes.addAll(
                    recipeManager.getRecipesFor(RecipeType.STONECUTTING, inventory, level));
        }

        // Deselect a recipe that is now unavailable
        if (stonecuttingRecipeId != null
                && stonecuttingRecipes.stream().noneMatch(r -> r.getId().equals(stonecuttingRecipeId))) {
            stonecuttingRecipeId = null;
        }
    }

    public void clear() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR);
            return;
        }

        encodedInputsInv.clear();
        encodedOutputsInv.clear();

        this.broadcastChanges();
        this.getAndUpdateOutput();
    }

    @Override
    public InternalInventory getCraftingMatrix() {
        return encodedInputsInv.createMenuWrapper().getSubInventory(0, CRAFTING_GRID_SLOTS);
    }

    @Override
    public boolean useRealItems() {
        return false;
    }

    public void setMode(EncodingMode mode) {
        if (this.mode != mode && mode == EncodingMode.STONECUTTING) {
            updateStonecuttingRecipes();
        }

        if (isClientSide()) {
            sendClientAction(ACTION_SET_MODE, mode);
        } else {
            this.mode = mode;
        }
    }

    public void setSubstitute(boolean substitute) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_SUBSTITUTION, substitute);
        } else {
            this.substitute = substitute;
        }
    }

    public void setSubstituteFluids(boolean substituteFluids) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_FLUID_SUBSTITUTION, substituteFluids);
        } else {
            this.substituteFluids = substituteFluids;
        }
    }

    public @Nullable ResourceLocation getStonecuttingRecipeId() {
        return stonecuttingRecipeId;
    }

    public void setStonecuttingRecipeId(ResourceLocation id) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_STONECUTTING_RECIPE_ID, id);
        } else {
            this.encodingLogic.setStonecuttingRecipeId(id);
        }
    }

    @Override
    protected ItemStack transferStackToMenu(ItemStack input) {

        if (Utils.quickInsert(encodingLogic.getEncodedPatternInv(), input) ) {
            return ItemStack.EMPTY;
        }

        return super.transferStackToMenu(input);
    }

    @Contract("null -> false")
    public boolean canModifyAmountForSlot(@Nullable Slot slot) {
        return isProcessingPatternSlot(slot) && slot.hasItem();
    }

    @Contract("null -> false")
    public boolean isProcessingPatternSlot(@Nullable Slot slot) {
        if (slot == null || mode != EncodingMode.PROCESSING) {
            return false;
        }

        for (var processingOutputSlot : processingOutputSlots) {
            if (processingOutputSlot == slot) {
                return true;
            }
        }

        for (var craftingSlot : processingInputSlots) {
            if (craftingSlot == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cycles the defined processing outputs around in case recipe transfer didn't put what the player considers the
     * primary output into the right slot.
     */
    public void cycleProcessingOutput() {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_PROCESSING_OUTPUT);
        } else {
            if (mode != EncodingMode.PROCESSING) {
                return;
            }

            var newOutputs = new ItemStack[getProcessingOutputSlots().length];
            for (int i = 0; i < processingOutputSlots.length; i++) {
                newOutputs[i] = ItemStack.EMPTY;
                if (!processingOutputSlots[i].getItem().isEmpty()) {
                    // Search for the next, skipping empty slots
                    for (int j = 1; j < processingOutputSlots.length; j++) {
                        var nextItem = processingOutputSlots[(i + j) % processingOutputSlots.length].getItem();
                        if (!nextItem.isEmpty()) {
                            newOutputs[i] = nextItem;
                            break;
                        }
                    }
                }
            }

            for (int i = 0; i < newOutputs.length; i++) {
                processingOutputSlots[i].set(newOutputs[i]);
            }
        }
    }


    // Can cycle if there is more than 1 processing output encoded
    public boolean canCycleProcessingOutputs() {
        return mode == EncodingMode.PROCESSING
                && Arrays.stream(processingOutputSlots).filter(s -> !s.getItem().isEmpty()).count() > 1;
    }


    @Nullable
    private IGrid getGrid() {
        IActionHost host = this.getActionHost();
        if (host != null) {
            final IGridNode agn = host.getActionableNode();
            if (agn != null && agn.isActive()) {
                return agn.getGrid();
            }
        }
        return null;
    }

    private static class VisitorState {
        // Total number of pattern provider hosts found
        int total;
        // Set to true if any visited machines were missing from diList, or had a different name
        boolean forceFullUpdate;
    }

    private boolean isFull(PatternContainer logic) {
        for (int i = 0; i < logic.getTerminalPatternInventory().size(); i++) {
            if (logic.getTerminalPatternInventory().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isVisible(PatternContainer container) {
        boolean isVisible = container.isVisibleInTerminal();

        return switch (getShownPatternProviders()) {
            case VISIBLE -> isVisible;
            case NOT_FULL -> isVisible && (pinnedHosts.contains(container) || !isFull(container));
            case ALL -> true;
        };
    }

    private <T extends PatternContainer> void visitPatternProviderHosts(IGrid grid, Class<T> machineClass,
                                                                        VisitorState state) {
        for (var container : grid.getActiveMachines(machineClass)) {
            if (!isVisible(container)) {
                continue;
            }

            if (getShownPatternProviders() == ShowPatternProviders.NOT_FULL) {
                pinnedHosts.add(container);
            }

            var t = this.diList.get(container);
            if (t == null || !t.group.equals(container.getTerminalGroup())) {
                state.forceFullUpdate = true;
            }

            state.total++;
        }
    }

    public boolean recyclePattern(ItemStack stack){
        if(stack.isEmpty()) return false;
        ItemStack item = patternRecycleSlot.getItem();
        patternRecycleSlot.set(ItemStack.EMPTY);
        if(patternRecycleSlot.safeInsert(stack.copy()).isEmpty()
                && getGrid() != null
                && getGrid().getStorageService().getInventory().insert(
                AEItemKey.of(AEItems.BLANK_PATTERN),
                1,
                Actionable.MODULATE,
                getActionSource()) > 0){
            return true;
        } else {
            patternRecycleSlot.set(item);
            return false;
        }
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        if(id == 0){
            if (slot < 0 || slot >= this.slots.size()) {
                return;
            }
            var s = this.getSlot(slot);

            if(s == patternRecycleSlot){
                var carried = getCarried();
                if (action == InventoryAction.PICKUP_OR_SET_DOWN && !carried.isEmpty()) {
                    ItemStack inSlot = patternRecycleSlot.getItem();
                    if (inSlot.isEmpty()) {
                        setCarried(patternRecycleSlot.safeInsert(carried));
                    } else {

                        if(recyclePattern(carried.copy()))
                            setCarried(ItemStack.EMPTY);
                    }
                } else {
                    setCarried(patternRecycleSlot.getItem());
                    patternRecycleSlot.set(ItemStack.EMPTY);
                }
                return;
            }

            if(s instanceof PatternBufferSlot patternBufferSlot)
            {
                if(action == InventoryAction.EMPTY_ITEM
                        && recyclePattern(patternBufferSlot.getItem()))
                    patternBufferSlot.set(ItemStack.EMPTY);
                else if(action == InventoryAction.CREATIVE_DUPLICATE){
                    selectedPatternSlot = slot;
                    encodingLogic.loadEncodedPattern(patternBufferSlot.getItem());
                    encodingLogic.saveChanges();
                }
            }

            super.doAction(player, action, slot, id);
        }
        else {
            final ContainerTracker inv = this.byId.get(id);
            if (inv == null) {
                // Can occur if the client sent an interaction packet right before an inventory got removed
                return;
            }
            if(action == InventoryAction.FILL_ITEM){//this means quick transfer pattern
                if(slot == -1){
                    Utils.quickTransfer(encodingLogic.getEncodedPatternInv(), inv.server, false);
                }
                else if(slot == -2){
                    Utils.quickTransferAll(encodingLogic.getEncodedPatternInv(), inv.server);
                }
                else if(Utils.quickInsert(inv.server, getSlot(slot).getItem()))
                    getSlot(slot).set(ItemStack.EMPTY);

                if(selectedPatternSlot != -1 && getSlot(selectedPatternSlot).getItem().isEmpty())
                    selectedPatternSlot = -1;
                return;
            }

            if (slot < 0 || slot >= inv.server.size()) {
                // Client refers to an invalid slot. This should NOT happen
                AELog.warn("Client refers to invalid slot %d of inventory %s", slot, inv.container);
                return;
            }

            final ItemStack is = inv.server.getStackInSlot(slot);

            var patternSlot = new FilteredInternalInventory(inv.server.getSlotInv(slot), new PatternSlotFilter());

            var carried = getCarried();
            switch (action) {
                case PICKUP_OR_SET_DOWN -> {
                    if (!carried.isEmpty()) {
                        ItemStack inSlot = patternSlot.getStackInSlot(0);
                        if (inSlot.isEmpty()) {
                            setCarried(patternSlot.addItems(carried));
                        } else {
                            inSlot = inSlot.copy();
                            final ItemStack inHand = carried.copy();

                            patternSlot.setItemDirect(0, ItemStack.EMPTY);
                            setCarried(ItemStack.EMPTY);

                            setCarried(patternSlot.addItems(inHand.copy()));

                            if (getCarried().isEmpty()) {
                                setCarried(inSlot);
                            } else {
                                setCarried(inHand);
                                patternSlot.setItemDirect(0, inSlot);
                            }
                        }
                    } else {
                        setCarried(patternSlot.getStackInSlot(0));
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                    }
                }
                case SPLIT_OR_PLACE_SINGLE -> {
                    if(Utils.quickInsert(encodingLogic.getEncodedPatternInv(), patternSlot.getStackInSlot(0)))
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                }
                case SHIFT_CLICK -> {
                    var stack = patternSlot.getStackInSlot(0).copy();
                    if (!player.getInventory().add(stack)) {
                        patternSlot.setItemDirect(0, stack);
                    } else {
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                    }
                }
                case MOVE_REGION -> {
                    for (int x = 0; x < inv.server.size(); x++) {
                        var stack = inv.server.getStackInSlot(x);
                        if (!player.getInventory().add(stack)) {
                            patternSlot.setItemDirect(0, stack);
                        } else {
                            patternSlot.setItemDirect(0, ItemStack.EMPTY);
                        }
                    }
                }
                case CREATIVE_DUPLICATE -> {
                    if (player.getAbilities().instabuild && carried.isEmpty()) {
                        setCarried(is.isEmpty() ? ItemStack.EMPTY : is.copy());
                    }
                }
                case PICKUP_SINGLE -> {
                    if(recyclePattern(patternSlot.getStackInSlot(0))){
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    private void sendFullUpdate(@Nullable IGrid grid) {
        this.byId.clear();
        this.diList.clear();

        sendPacketToClient(new ClearPatternAccessTerminalPacket());

        if (grid == null) {
            return;
        }

        for (var machineClass : grid.getMachineClasses()) {
            var containerClass = tryCastMachineToContainer(machineClass);
            if (containerClass == null) {
                continue;
            }

            for (var container : grid.getActiveMachines(containerClass)) {
                if (isVisible(container)) {
                    this.diList.put(container, new ContainerTracker(container,
                            container.getTerminalPatternInventory(),
                            container.getTerminalGroup()));
                }
            }
        }

        for (var inv : this.diList.values()) {
            this.byId.put(inv.serverId, inv);
            sendPacketToClient(inv.createFullPacket());
        }

        if (this.getPlayer() instanceof ServerPlayer player) {
            for (var inv : diList.values()) {
                var id = inv.serverId;
                var container = inv.container;
                if (container instanceof BlockEntity te) {
                    EPPNetworkHandler.INSTANCE.sendTo(new SExPatternInfo(id, te.getBlockPos(), Objects.requireNonNull(te.getLevel()).dimension()), player);
                } else if (container instanceof AEBasePart part) {
                    EPPNetworkHandler.INSTANCE.sendTo(new SExPatternInfo(id, part.getBlockEntity().getBlockPos(), Objects.requireNonNull(part.getLevel()).dimension(), part.getSide()), player);
                } else if (LoadList.GT && MetaTileResolver.check(container)) {
                    EPPNetworkHandler.INSTANCE.sendTo(new SExPatternInfo(id, MetaTileResolver.getBlockPos(container), MetaTileResolver.getLevel(container).dimension()), player);
                }
            }
        }
    }

    private void sendIncrementalUpdate() {
        for (var inv : this.diList.values()) {
            var packet = inv.createUpdatePacket();
            if (packet != null) {
                sendPacketToClient(packet);
            }
        }
    }

    public void tpToProvider(Long serverId) {
        if (isClientSide()) {
            sendClientAction(ACTION_TP_TO_PROVIDER, serverId);
        } else {
            var entry = byId.get(serverId.longValue());
            if (entry == null) {
                return;
            }

            Object container = entry.container;

            BlockPos originPos = null;
            ResourceKey<Level> levelKey = null;

            if (container instanceof BlockEntity te) {
                originPos = te.getBlockPos();
                levelKey = Objects.requireNonNull(te.getLevel()).dimension();

            } else if (container instanceof AEBasePart part) {
                originPos = part.getBlockEntity().getBlockPos();
                levelKey = Objects.requireNonNull(part.getLevel()).dimension();

            } else if (LoadList.GT && MetaTileResolver.check(container)) {
                originPos = MetaTileResolver.getBlockPos(container);
                levelKey = MetaTileResolver.getLevel(container).dimension();
            }

            if (originPos == null) {
                return;
            }

            var player = getPlayer();
            if (player == null) {
                return;
            }

            var server = player.getServer();
            if (server == null) {
                return;
            }

            ServerLevel targetLevel = server.getLevel(levelKey);
            if (targetLevel == null) {
                return;
            }

            // 搜索安全位置
            BlockPos safePos = Utils.findSafeTeleportPos(targetLevel, originPos);

            Vec3 feetPos = new Vec3(
                    safePos.getX() + 0.5,
                    safePos.getY(),
                    safePos.getZ() + 0.5
            );

            Vec3 lookTarget = Vec3.atCenterOf(originPos);

            float[] rot = Utils.getLookAtRotation((ServerPlayer) getPlayer(),feetPos, lookTarget);

            // 执行传送（中心点）
            player.teleportTo(
                    targetLevel,
                    safePos.getX() + 0.5,
                    safePos.getY(),
                    safePos.getZ() + 0.5,
                    Set.of(),
                    rot[0],
                    rot[1]
            );

        }


    }



    private static class ContainerTracker {

        private final PatternContainer container;
        private final long sortBy;
        private final long serverId = inventorySerial++;
        private final PatternContainerGroup group;
        // This is used to track the inventory contents we sent to the client for change detection
        private final InternalInventory client;
        // This is a reference to the real inventory used by this machine
        private final InternalInventory server;

        public ContainerTracker(PatternContainer container, InternalInventory patterns, PatternContainerGroup group) {
            this.container = container;
            this.server = patterns;
            this.client = new AppEngInternalInventory(this.server.size());
            this.group = group;
            this.sortBy = container.getTerminalSortOrder();
        }

        public PatternAccessTerminalPacket createFullPacket() {
            var slots = new Int2ObjectArrayMap<ItemStack>(server.size());
            for (int i = 0; i < server.size(); i++) {
                var stack = server.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    slots.put(i, stack);
                }
            }

            return PatternAccessTerminalPacket.fullUpdate(
                    serverId,
                    server.size(),
                    sortBy,
                    group,
                    slots);
        }

        @Nullable
        public PatternAccessTerminalPacket createUpdatePacket() {
            var changedSlots = detectChangedSlots();
            if (changedSlots == null) {
                return null;
            }

            var slots = new Int2ObjectArrayMap<ItemStack>(changedSlots.size());
            for (int i = 0; i < changedSlots.size(); i++) {
                var slot = changedSlots.getInt(i);
                var stack = server.getStackInSlot(slot);
                // "update" client side.
                client.setItemDirect(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                slots.put(slot, stack);
            }

            return PatternAccessTerminalPacket.incrementalUpdate(
                    serverId,
                    slots);
        }

        @Nullable
        private IntList detectChangedSlots() {
            IntList changedSlots = null;
            for (int x = 0; x < server.size(); x++) {
                if (isDifferent(server.getStackInSlot(x), client.getStackInSlot(x))) {
                    if (changedSlots == null) {
                        changedSlots = new IntArrayList();
                    }
                    changedSlots.add(x);
                }
            }
            return changedSlots;
        }

        private static boolean isDifferent(ItemStack a, ItemStack b) {
            if (a.isEmpty() && b.isEmpty()) {
                return false;
            }

            if (a.isEmpty() || b.isEmpty()) {
                return true;
            }

            return !ItemStack.matches(a, b);
        }
    }

    public static class PatternSlotFilter implements IAEItemFilter {

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && PatternDetailsHelper.isEncodedPattern(stack);
        }
    }

    private static Class<? extends PatternContainer> tryCastMachineToContainer(Class<?> machineClass) {
        if (PatternContainer.class.isAssignableFrom(machineClass)) {
            return machineClass.asSubclass(PatternContainer.class);
        }
        return null;
    }
}
