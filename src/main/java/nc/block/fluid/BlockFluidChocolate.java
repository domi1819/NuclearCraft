package nc.block.fluid;

import nc.fluid.FluidChocolate;
import nc.util.PotionHelper;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

public class BlockFluidChocolate extends BlockFluid {

	public BlockFluidChocolate(Fluid fluid) {
		super(fluid, Material.WATER);
		setQuantaPerBlock(6);
	}
	
	public BlockFluidChocolate(FluidChocolate fluid) {
		super(fluid, Material.WATER);
		setQuantaPerBlock(6);
	}
	
	@Override
	public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
		if (entityIn instanceof EntityLivingBase) {
			((EntityLivingBase) entityIn).addPotionEffect(PotionHelper.newEffect(2, 1, 100));
			((EntityLivingBase) entityIn).addPotionEffect(PotionHelper.newEffect(10, 1, 100));
		}
	}
}
