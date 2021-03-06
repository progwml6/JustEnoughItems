package mezz.jei.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.recipe.IRecipeCategory;

/**
 * A RecipeMap efficiently links Recipes, IRecipeCategory, and ItemStacks.
 */
public class RecipeMap {

	@Nonnull
	private final Table<IRecipeCategory, String, List<Object>> recipeTable = HashBasedTable.create();
	@Nonnull
	private final ArrayListMultimap<String, IRecipeCategory> categoryMap = ArrayListMultimap.create();
	@Nonnull
	private final Ordering<IRecipeCategory> recipeCategoryOrdering;

	public RecipeMap(final RecipeCategoryComparator recipeCategoryComparator) {
		this.recipeCategoryOrdering = Ordering.from(recipeCategoryComparator);
	}

	@Nonnull
	public ImmutableList<IRecipeCategory> getRecipeCategories(@Nonnull ItemStack itemStack) {
		List<IRecipeCategory> recipeCategories = new ArrayList<>();
		for (String stackKey : getNamesWithWildcard(itemStack)) {
			recipeCategories.addAll(categoryMap.get(stackKey));
		}
		return recipeCategoryOrdering.immutableSortedCopy(recipeCategories);
	}

	@Nonnull
	public ImmutableList<IRecipeCategory> getRecipeCategories(@Nonnull Fluid fluid) {
		String key = getKeyForFluid(fluid);
		return recipeCategoryOrdering.immutableSortedCopy(categoryMap.get(key));
	}

	private void addRecipeCategory(@Nonnull IRecipeCategory recipeCategory, @Nonnull ItemStack itemStack) {
		String stackKey = StackUtil.uniqueIdentifierForStack(itemStack, false);
		List<IRecipeCategory> recipeCategories = categoryMap.get(stackKey);
		if (!recipeCategories.contains(recipeCategory)) {
			recipeCategories.add(recipeCategory);
		}
	}

	private void addRecipeCategory(@Nonnull IRecipeCategory recipeCategory, @Nonnull Fluid fluid) {
		String key = getKeyForFluid(fluid);
		List<IRecipeCategory> recipeCategories = categoryMap.get(key);
		if (!recipeCategories.contains(recipeCategory)) {
			recipeCategories.add(recipeCategory);
		}
	}

	@Nonnull
	private List<String> getNamesWithWildcard(@Nonnull ItemStack itemStack) {
		List<String> names = new ArrayList<>(2);
		names.add(StackUtil.uniqueIdentifierForStack(itemStack, false));
		names.add(StackUtil.uniqueIdentifierForStack(itemStack, true));
		return names;
	}

	@Nonnull
	private String getKeyForFluid(Fluid fluid) {
		return "fluid:" + fluid.getID();
	}

	@Nonnull
	public ImmutableList<Object> getRecipes(@Nonnull IRecipeCategory recipeCategory, @Nonnull ItemStack stack) {
		Map<String, List<Object>> recipesForType = recipeTable.row(recipeCategory);

		ImmutableList.Builder<Object> listBuilder = ImmutableList.builder();
		for (String name : getNamesWithWildcard(stack)) {
			List<Object> recipes = recipesForType.get(name);
			if (recipes != null) {
				listBuilder.addAll(recipes);
			}
		}
		return listBuilder.build();
	}

	@Nonnull
	public ImmutableList<Object> getRecipes(@Nonnull IRecipeCategory recipeCategory, @Nonnull Fluid fluid) {
		Map<String, List<Object>> recipesForType = recipeTable.row(recipeCategory);

		String name = getKeyForFluid(fluid);
		List<Object> recipes = recipesForType.get(name);
		if (recipes == null) {
			return ImmutableList.of();
		}
		return ImmutableList.copyOf(recipes);
	}

	public void addRecipe(@Nonnull Object recipe, @Nonnull IRecipeCategory recipeCategory, @Nonnull Iterable<ItemStack> itemStacks, @Nonnull Iterable<FluidStack> fluidStacks) {
		Map<String, List<Object>> recipesForType = recipeTable.row(recipeCategory);

		for (ItemStack itemStack : itemStacks) {
			if (itemStack == null) {
				continue;
			}

			String stackKey = StackUtil.uniqueIdentifierForStack(itemStack, false);
			List<Object> recipes = recipesForType.get(stackKey);
			if (recipes == null) {
				recipes = Lists.newArrayList();
				recipesForType.put(stackKey, recipes);
			}
			recipes.add(recipe);

			addRecipeCategory(recipeCategory, itemStack);
		}

		for (FluidStack fluidStack : fluidStacks) {
			if (fluidStack == null) {
				continue;
			}
			Fluid fluid = fluidStack.getFluid();
			if (fluid == null) {
				continue;
			}

			String fluidKey = getKeyForFluid(fluid);
			List<Object> recipes = recipesForType.get(fluidKey);
			if (recipes == null) {
				recipes = Lists.newArrayList();
				recipesForType.put(fluidKey, recipes);
			}
			recipes.add(recipe);

			addRecipeCategory(recipeCategory, fluid);
		}
	}
}
