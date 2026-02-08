package com.ctnh.ae2pw.integration.emi;

import appeng.api.stacks.*;
import appeng.core.AEConfig;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.emi.EmiStackHelper;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.me.common.GridInventoryEntry;

import com.ctnh.ae2pw.client.screen.PatternWorkStationScreen;
import com.ctnh.ae2pw.common.MEStorageMenu;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;

import com.ctnh.ae2pw.integration.EncodingHelper;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.recipe.handler.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.*;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static appeng.integration.modules.jeirei.TransferHelper.*;

public class EmiPatternWorkstationHandler implements StandardRecipeHandler<PatternWorkStationMenu> {

    private static final int GRID_W = 3;
    private static final int GRID_H = 3;

    /* ------------------------------------------------------------ */
    /* Slot / Inventory                                             */
    /* ------------------------------------------------------------ */

    @Override
    public List<Slot> getInputSources(PatternWorkStationMenu menu) {
        var slots = new ArrayList<Slot>();
        slots.addAll(menu.getSlots(SlotSemantics.PLAYER_INVENTORY));
        slots.addAll(menu.getSlots(SlotSemantics.PLAYER_HOTBAR));
        slots.addAll(menu.getSlots(SlotSemantics.CRAFTING_GRID));
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(PatternWorkStationMenu menu) {
        return menu.getSlots(SlotSemantics.CRAFTING_GRID);
    }

    @Override
    public @Nullable Slot getOutputSlot(PatternWorkStationMenu menu) {
        return menu.getSlots(SlotSemantics.CRAFTING_RESULT).stream().findFirst().orElse(null);
    }

    /* ------------------------------------------------------------ */
    /* Inventory exposure                                           */
    /* ------------------------------------------------------------ */

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<PatternWorkStationMenu> screen) {

        if (!AEConfig.instance().isExposeNetworkInventoryToEmi()) {
            return StandardRecipeHandler.super.getInventory(screen);
        }

        var list = new ArrayList<EmiStack>();
        var menu = screen.getMenu();

        for (Slot slot : getInputSources(menu)) {
            list.add(EmiStack.of(slot.getItem()));
        }

        var repo = menu.getClientRepo();

        if (repo != null) {
            for (var entry : repo.getAllEntries()) {

                if (entry.getStoredAmount() <= 0) continue;

                var emi = EmiStackHelper.toEmiStack(
                        new GenericStack(entry.getWhat(), entry.getStoredAmount()));

                if (emi != null) list.add(emi);
            }
        }

        return new EmiPlayerInventory(list);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return true;
    }

    /* ------------------------------------------------------------ */
    /* Craft entry                                                  */
    /* ------------------------------------------------------------ */

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<PatternWorkStationMenu> context) {
        return transferRecipe(recipe, context, true).canCraft();
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<PatternWorkStationMenu> context) {
        return true;
    }

    /* ------------------------------------------------------------ */
    /* Transfer logic                                               */
    /* ------------------------------------------------------------ */

    private Result transferRecipe(EmiRecipe emiRecipe,
                                  EmiCraftContext<PatternWorkStationMenu> context,
                                  boolean doTransfer) {

        var menu = context.getScreenHandler();

        Recipe<?> holder = getRecipeHolder(menu.getPlayer().level(), emiRecipe);

        var result = transferRecipe((PatternWorkStationScreen) context.getScreen(), holder, emiRecipe, doTransfer);

        if (result instanceof Result.Success && doTransfer) {

            if (AbstractContainerScreen.hasShiftDown()) {
                menu.encode(0L);
            } else if (AbstractContainerScreen.hasControlDown()) {


                var screen = (PatternWorkStationScreen) context.getScreen();
                screen.updateBeforeRender();

                var id = screen.findContainer(ItemStack.EMPTY);
                menu.encode(id);
            }

            Minecraft.getInstance().setScreen(context.getScreen());
        }

        return result;
    }

    private Result transferRecipe(PatternWorkStationScreen screen,
                                  @Nullable Recipe<?> recipe,
                                  EmiRecipe emiRecipe,
                                  boolean doTransfer) {

        boolean crafting = isCraftingRecipe(recipe, emiRecipe);

        if (crafting && !fits3x3(recipe)) {
            return Result.failed(ItemModText.RECIPE_TOO_LARGE.text());
        }

        var menu = screen.getMenu();

        if (doTransfer) {

            if (crafting) {
                screen.setShowCrafting(true);
                screen.fillSearchPattern("");
                EncodingHelper.encodeCraftingRecipe(menu,
                        recipe,
                        getGuiIngredients(emiRecipe),
                        s -> true);
            } else {
                screen.setShowCrafting(false);
                screen.fillSearchPattern(emiRecipe.getCategory().getName().getString());
                EncodingHelper.encodeProcessingRecipe(menu,
                        EmiStackHelper.ofInputs(emiRecipe),
                        EmiStackHelper.ofOutputs(emiRecipe));
            }

            return Result.success();
        }

        /* ---------- preview craftable ---------- */

        var repo = menu.getClientRepo();

        Set<AEKey> craftable = repo != null
                ? repo.getAllEntries().stream()
                .filter(GridInventoryEntry::isCraftable)
                .map(GridInventoryEntry::getWhat)
                .collect(Collectors.toSet())
                : Set.of();

        return new Result.EncodeWithCraftables(craftable);
    }

    /* ------------------------------------------------------------ */
    /* Tooltip / Render                                             */
    /* ------------------------------------------------------------ */

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe,
                                                   EmiCraftContext<PatternWorkStationMenu> context) {

        var tooltip = transferRecipe(recipe, context, false)
                .getTooltip(recipe, context);

        if (tooltip == null) {
            return StandardRecipeHandler.super.getTooltip(recipe, context);
        }

        return tooltip.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
    }

    @Override
    public void render(EmiRecipe recipe,
                       EmiCraftContext<PatternWorkStationMenu> context,
                       List<Widget> widgets,
                       GuiGraphics draw) {

        transferRecipe(recipe, context, false)
                .render(recipe, context, widgets, draw);
    }

    /* ------------------------------------------------------------ */
    /* Helpers                                                      */
    /* ------------------------------------------------------------ */

    private static boolean isCraftingRecipe(Recipe<?> recipe, EmiRecipe emiRecipe) {
        return EncodingHelper.isSupportedCraftingRecipe(recipe)
                || emiRecipe.getCategory().equals(VanillaEmiRecipeCategories.CRAFTING);
    }

    private static boolean fits3x3(Recipe<?> recipe) {
        return recipe == null || recipe.canCraftInDimensions(GRID_W, GRID_H);
    }

    private static Recipe<?> getRecipeHolder(Level level, EmiRecipe recipe) {

        if (recipe.getBackingRecipe() != null) return recipe.getBackingRecipe();

        if (recipe.getId() != null) {
            return level.getRecipeManager().byKey(recipe.getId()).orElse(null);
        }

        return null;
    }

    private List<List<GenericStack>> getGuiIngredients(EmiRecipe recipe) {

        var result = new ArrayList<List<GenericStack>>(GRID_W * GRID_H);

        for (int i = 0; i < GRID_W * GRID_H; i++) {

            var list = new ArrayList<GenericStack>();

            if (i < recipe.getInputs().size()) {
                for (var emiStack : recipe.getInputs().get(i).getEmiStacks()) {

                    var gs = EmiStackHelper.toGenericStack(emiStack);

                    if (gs != null && gs.what() instanceof AEItemKey) {
                        list.add(gs);
                    }
                }
            }

            result.add(list);
        }

        return result;
    }

    /* ------------------------------------------------------------ */
    /* Result                                                       */
    /* ------------------------------------------------------------ */

    private sealed static abstract class Result {

        @Nullable
        List<Component> getTooltip(EmiRecipe recipe, EmiCraftContext<?> ctx) { return null; }

        abstract boolean canCraft();

        void render(EmiRecipe recipe,
                    EmiCraftContext<? extends AEBaseMenu> ctx,
                    List<Widget> widgets,
                    GuiGraphics g) {}

        /* ---------- factory ---------- */

        static Success success() { return new Success(); }

        static Error
        failed(Component text) { return new Error(text); }

        /* ---------- types ---------- */

        static final class Success extends Result {
            boolean canCraft() { return true; }
        }

        static final class Error extends Result {

            @Getter
            private final Component msg;

            Error(Component msg) { this.msg = msg; }

            boolean canCraft() { return false; }
        }

        static final class EncodeWithCraftables extends Result {

            private final Set<AEKey> craftable;

            EncodeWithCraftables(Set<AEKey> craftable) {
                this.craftable = craftable;
            }

            boolean canCraft() { return true; }

            @Override
            void render(EmiRecipe recipe,
                        EmiCraftContext<? extends AEBaseMenu> ctx,
                        List<Widget> widgets,
                        GuiGraphics g) {

                for (Widget w : widgets) {

                    if (w instanceof SlotWidget slot && slot.getRecipe() == null) {

                        if (isCraftable(slot.getStack())) {

                            var pose = g.pose();
                            pose.pushPose();
                            pose.translate(0, 0, 400);

                            var b = inner(slot);
                            g.fill(b.x(), b.y(), b.right(), b.bottom(),
                                    BLUE_SLOT_HIGHLIGHT_COLOR);

                            pose.popPose();
                        }
                    }
                }
            }

            private boolean isCraftable(EmiIngredient ing) {

                return ing.getEmiStacks().stream().anyMatch(stack -> {

                    var gs = EmiStackHelper.toGenericStack(stack);
                    return gs != null && craftable.contains(gs.what());
                });
            }
        }
    }

    private static Bounds inner(SlotWidget slot) {

        var b = slot.getBounds();

        return new Bounds(
                b.x() + 1,
                b.y() + 1,
                b.width() - 2,
                b.height() - 2);
    }
}
