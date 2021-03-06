package nc.render;

import javax.annotation.Nullable;

import nc.block.fluid.BlockFluidBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class ColorRenderer {
	
	public static class FluidBlockColor implements IBlockColor {
		
		final BlockFluidBase fluidBlock;
		
		public FluidBlockColor(BlockFluidBase fluidBlock) {
			this.fluidBlock = fluidBlock;	
		}

		@Override
		public int colorMultiplier(IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) {
			if(tintIndex == 0) {
				return fluidBlock.fluid.getColor();
			}
			return 0xFFFFFF;
		}
	}
	
	public static class FluidItemBlockColor implements IItemColor {
		
		final BlockFluidBase fluidBlock;
		
		public FluidItemBlockColor(BlockFluidBase fluidBlock) {
			this.fluidBlock = fluidBlock;	
		}

		@Override
		public int colorMultiplier(ItemStack stack, int tintIndex) {
			if(tintIndex == 0) {
				return fluidBlock.fluid.getColor();
			}
			return 0xFFFFFF;
		}
	}
}
