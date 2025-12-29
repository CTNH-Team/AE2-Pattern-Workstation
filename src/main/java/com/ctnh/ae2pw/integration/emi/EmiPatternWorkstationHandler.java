package com.ctnh.ae2pw.integration.emi;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEBlocks;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.emi.EmiStackHelper;
import appeng.menu.me.common.GridInventoryEntry;
import com.ctnh.ae2pw.client.screen.PatternWorkStationScreen;
import com.ctnh.ae2pw.common.PatternWorkStationMenu;
import com.ctnh.ae2pw.integration.EncodingHelper;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EmiPatternWorkstationHandler extends AbstractRecipeHandler<PatternWorkStationMenu> {
    EmiPatternWorkstationHandler(Class<PatternWorkStationMenu> containerClass) {
        super(containerClass);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<PatternWorkStationMenu> context) {
        return true;
    }

    @Override
    protected Result transferRecipe(EmiRecipe emiRecipe, EmiCraftContext<PatternWorkStationMenu> context, boolean doTransfer) {
        var result = super.transferRecipe(emiRecipe, context, doTransfer);
        if (result instanceof Result.Success && doTransfer) {
            if(context.getAmount() > 1){
                context.getScreenHandler().encode(0L);
            }
            if(context.getDestination() == EmiCraftContext.Destination.CURSOR){
                var id = ((PatternWorkStationScreen)context.getScreen()).findContainer(ItemStack.EMPTY);
                context.getScreenHandler().encode(id);

            }

        }
        return result;
    }

    @Override
    protected Result transferRecipe(PatternWorkStationMenu menu, @Nullable Recipe<?> recipeBase, EmiRecipe emiRecipe, boolean doTransfer) {
        // Recipe displays can be based on anything. Not just Recipe<?>
        Recipe<?> recipe = null;
        if (recipeBase instanceof Recipe<?>) {
            recipe = recipeBase;
        }

        // Crafting recipe slots are not grouped, hence they must fit into the 3x3 grid.
        boolean craftingRecipe = isCraftingRecipe(recipe, emiRecipe);
        if (craftingRecipe && !fitsIn3x3Grid(recipe, emiRecipe)) {
            return Result.createFailed(ItemModText.RECIPE_TOO_LARGE.text());
        }

        if (doTransfer) {
            if (craftingRecipe) {
                EncodingHelper.encodeCraftingRecipe(menu,
                        recipe,
                        getGuiIngredientsForCrafting(emiRecipe),
                        stack -> true);
                menu.patternSearch = AEBlocks.MOLECULAR_ASSEMBLER.block().getName().getString();
            } else {
                EncodingHelper.encodeProcessingRecipe(menu,
                        EmiStackHelper.ofInputs(emiRecipe),
                        EmiStackHelper.ofOutputs(emiRecipe));
                menu.patternSearch = emiRecipe.getCategory().getName().getString();
            }

        } else {
            var repo = menu.getClientRepo();
            Set<AEKey> craftableKeys = repo != null ? repo.getAllEntries().stream()
                    .filter(GridInventoryEntry::isCraftable)
                    .map(GridInventoryEntry::getWhat)
                    .collect(Collectors.toSet()) : Set.of();

            return new Result.EncodeWithCraftables(craftableKeys);
        }

        return Result.createSuccessful();
    }

    private List<List<GenericStack>> getGuiIngredientsForCrafting(EmiRecipe emiRecipe) {
        var result = new ArrayList<List<GenericStack>>(CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT);
        for (int i = 0; i < CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT; i++) {
            var stacks = new ArrayList<GenericStack>();

            if (i < emiRecipe.getInputs().size()) {
                for (var emiStack : emiRecipe.getInputs().get(i).getEmiStacks()) {
                    var genericStack = EmiStackHelper.toGenericStack(emiStack);
                    if (genericStack != null && genericStack.what() instanceof AEItemKey) {
                        stacks.add(genericStack);
                    }
                }
            }

            result.add(stacks);
        }

        return result;
    }
}
