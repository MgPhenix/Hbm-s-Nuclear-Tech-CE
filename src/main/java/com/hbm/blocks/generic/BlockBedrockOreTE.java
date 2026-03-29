package com.hbm.blocks.generic;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.util.EnumUtil;
import com.hbm.util.I18nUtil;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import org.jetbrains.annotations.NotNull;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

public class BlockBedrockOreTE extends BlockContainer implements ILookOverlay {

	public BlockBedrockOreTE(String s) {
		super(Material.ROCK);
		this.setTranslationKey(s);
		this.setRegistryName(s);

		ModBlocks.ALL_BLOCKS.add(this);
	}

	//This shit was AI Generated (and modified by me) because idk what am doing will be removed when i properly learn how to do it (or maybe never change)
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
									EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		ItemStack stack = player.getHeldItem(hand);
		if(stack.isEmpty()) return false;
		if(!player.capabilities.isCreativeMode) return false;
		if(world.isRemote) return true;

		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileEntityBedrockOre ore) {

			if(stack.getItem() == ModItems.drillbit) {
				ItemEnums.EnumDrillType type = EnumUtil.grabEnumSafely(ItemEnums.EnumDrillType.class, stack.getItemDamage());
				ore.tier = type.tier;
			}

			else if(stack.getItem() instanceof IFillableItem item) {
				FluidType type = item.getFirstFluidType(stack);
				if(type != null) {
					ore.acidRequirement = new FluidStack(type, item.getFill(stack));
				}
			}

			else {
				ore.resource = stack.copy();
				ore.shape = world.rand.nextInt(10);
			}

			ore.markDirty();
			world.notifyBlockUpdate(pos, state, state, 3);
		}
		return true;
	}


	@Override
	public boolean canEntitySpawn(@NotNull IBlockState state, @NotNull Entity entityIn) {
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(@NotNull World world, int meta) {
		return new TileEntityBedrockOre();
	}

	@Override
	public @NotNull EnumBlockRenderType getRenderType(@NotNull IBlockState state){
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public void onEntityWalk(@NotNull World worldIn, @NotNull BlockPos pos, Entity entityIn) {
		entityIn.setFire(3);
	}

	@Override
	public void printHook(Pre event, World world, BlockPos pos) {

		TileEntity te = world.getTileEntity(pos);

		if(!(te instanceof TileEntityBedrockOre ore))
			return;

        List<String> text = new ArrayList<>();
		if(ore.resource != null) {
			text.add(ore.resource.getDisplayName());
		}
		text.add(I18nUtil.resolveKey("desc.tier", ore.tier));

		if(ore.acidRequirement != null) {
			text.add(I18nUtil.resolveKey("desc.requires", ore.acidRequirement.fill, ore.acidRequirement.type.getLocalizedName()));
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
	}

	@AutoRegister
	public static class TileEntityBedrockOre extends TileEntity {

		public ItemStack resource;
		public FluidStack acidRequirement;
		public int tier;
		public int color;
		public int shape;

		public TileEntityBedrockOre setStyle(int color, int shape) {
			this.color = color;
			this.shape = shape;
			return this;
		}

		@Override
		public void readFromNBT(@NotNull NBTTagCompound nbt) {
			super.readFromNBT(nbt);
			this.resource = new ItemStack(Item.getItemById(nbt.getInteger("0id")), nbt.getByte("size"), nbt.getShort("meta"));
			if(this.resource.isEmpty()) this.resource = new ItemStack(ModItems.powder_iron);
			FluidType type = Fluids.fromID(nbt.getInteger("fluid"));

			if(type != Fluids.NONE) {
				this.acidRequirement = new FluidStack(type, nbt.getInteger("amount"));
			}

			this.tier = nbt.getInteger("tier");
			this.color = nbt.getInteger("color");
			this.shape = nbt.getInteger("shape");
		}

		@Override
		public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound nbt) {
			super.writeToNBT(nbt);

			if(this.resource != null) {
				nbt.setInteger("0id", Item.getIdFromItem(this.resource.getItem()));
				nbt.setByte("size", (byte) this.resource.getCount());
				nbt.setShort("meta", (short) this.resource.getItemDamage());
			}

			if(this.acidRequirement != null) {
				nbt.setInteger("fluid", this.acidRequirement.type.getID());
				nbt.setInteger("amount", this.acidRequirement.fill);
			}

			nbt.setInteger("tier", this.tier);
			nbt.setInteger("color", this.color);
			nbt.setInteger("shape", this.shape);
			return nbt;
		}

		@Override
		public SPacketUpdateTileEntity getUpdatePacket(){
			return new SPacketUpdateTileEntity(this.getPos(), 0, this.writeToNBT(new NBTTagCompound()));
		}

		@Override
		public @NotNull NBTTagCompound getUpdateTag() {
			return this.writeToNBT(new NBTTagCompound());
		}

		@Override
		public void onDataPacket(@NotNull NetworkManager net, SPacketUpdateTileEntity pkt) {
			this.readFromNBT(pkt.getNbtCompound());
			if(color == 0) {
				this.color = MainRegistry.proxy.getStackColor(resource, true);
			}
			world.markBlockRangeForRenderUpdate(pos, pos);
		}



	}
}
