package nc.tab;

import nc.init.NCItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabRadiation extends CreativeTabs {

	public TabRadiation() {
		super("nuclearcraftRadiation");
	}

	@Override
	public ItemStack getTabIconItem() {
		return new ItemStack(NCItems.geiger_counter);
	}
}
