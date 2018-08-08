package nc.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import nc.config.NCConfig;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class OreDictHelper {
	
	public static final List<String> INGOT_VOLUME_TYPES = Lists.newArrayList("ingot", "dust");
	public static final List<String> NUGGET_VOLUME_TYPES = Lists.newArrayList("nugget", "tinyDust");
	
	public static final List<String> GEM_VOLUME_TYPES = Lists.newArrayList("gem", "dust");
	public static final List<String> GEM_NUGGET_VOLUME_TYPES = Lists.newArrayList("nugget", "tinyDust");
	
	public static final List<String> DUST_VOLUME_TYPES = Lists.newArrayList("dust");
	public static final List<String> TINYDUST_VOLUME_TYPES = Lists.newArrayList("tinyDust");
	
	public static final List<String> FUEL_VOLUME_TYPES = Lists.newArrayList("fuel", "dust");
	
	public static final List<String> BLOCK_VOLUME_TYPES = Lists.newArrayList("block");
	
	public static boolean isOreMember(ItemStack stack, String oreName) {
		for (ItemStack ore : OreDictionary.getOres(oreName)) {
			if (ItemStack.areItemsEqual(ore, stack)) return true;
		}
		return false;
	}
	
	public static boolean oreExists(String ore) {
		return !OreDictionary.getOres(ore).isEmpty();
	}
	
	public static String getOreNameFromStacks(List<ItemStack> stackList) {
		List<Integer> idList = new ArrayList<Integer>();
		if (stackList.isEmpty() || stackList == null) return "Unknown";
		idList.addAll(ArrayHelper.asIntegerList(OreDictionary.getOreIDs(stackList.get(0))));
		
		for (ItemStack stack : stackList) {
			if (stack.isEmpty() || stack == null) return "Unknown";
			idList = ArrayHelper.intersect(idList, ArrayHelper.asIntegerList(OreDictionary.getOreIDs(stack)));
			if (idList.isEmpty()) return "Unknown";
		}
		return OreDictionary.getOreName(idList.get(0));
	}
	
	public static boolean getBlockMatchesOre(World world, BlockPos pos, String... names) {
		List<ItemStack> stackList = new ArrayList<ItemStack>();
		for (int i = 0; i < names.length; i++) {
			List<ItemStack> stacks = OreDictionary.getOres(names[i]);
			stackList.addAll(stacks);
		}
		ItemStack stack = ItemStackHelper.blockStateToStack(world.getBlockState(pos));
		for (ItemStack oreStack : stackList) if (oreStack.isItemEqual(stack)) return true;
		return false;
	}
	
	public static List<ItemStack> getPrioritisedStackList(String ore) {
		List<ItemStack> defaultStackList = new ArrayList<ItemStack>(OreDictionary.getOres(ore));
		if (!NCConfig.ore_dict_priority_bool || NCConfig.ore_dict_priority.length < 1) return defaultStackList;
		List<ItemStack> prioritisedStackList = new ArrayList<ItemStack>();
		for (int i = 0; i < NCConfig.ore_dict_priority.length; i++) {
			for (ItemStack stack : defaultStackList) {
				if (RegistryHelper.getModID(stack).equals(NCConfig.ore_dict_priority[i]) && !prioritisedStackList.contains(stack)) {
					prioritisedStackList.add(stack);
				}
			}
		}
		if (prioritisedStackList.isEmpty()) return defaultStackList;
		for (ItemStack stack : defaultStackList) {
			if (!prioritisedStackList.contains(stack)) {
				prioritisedStackList.add(stack);
			}
		}
		return prioritisedStackList;
	}
	
	public static ItemStack getPrioritisedCraftingStack(ItemStack backup, String ore) {
		List<ItemStack> stackList = getPrioritisedStackList(ore);
		if (stackList == null || stackList.isEmpty()) return backup;
		ItemStack stack = stackList.get(0).copy();
		stack.setCount(backup.getCount());
		return stack;
	}
	
	public static ItemStack getPrioritisedCraftingStack(Item backup, String ore) {
		return getPrioritisedCraftingStack(new ItemStack(backup), ore);
	}
	
	public static ItemStack getPrioritisedCraftingStack(Block backup, String ore) {
		return getPrioritisedCraftingStack(new ItemStack(backup), ore);
	}
}