package com.hbm.blocks.turret;

import com.hbm.lib.InventoryHelper;
import com.hbm.lib.NTMBlockContainer;
import com.hbm.tileentity.turret.TileEntityTurretSentry;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class TurretSentry extends NTMBlockContainer {

  public TurretSentry(Material material, String name) {
    super(material, name);
  }

  @Override
  public TileEntity createNewTileEntity(@NotNull World world, int meta) {
    return new TileEntityTurretSentry();
  }

  @Override
  public boolean onBlockActivated(
      @NotNull World world,
      @NotNull BlockPos pos,
      @NotNull IBlockState state,
      @NotNull EntityPlayer player,
      @NotNull EnumHand hand,
      @NotNull EnumFacing facing,
      float hitX,
      float hitY,
      float hitZ) {

    return super.standardOpenBehavior(world, pos.getX(), pos.getY(), pos.getZ(), player, 0);
  }

  @Override
  public @NotNull EnumBlockRenderType getRenderType(@NotNull IBlockState state) {
    return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
  }

  @Override
  public boolean isOpaqueCube(@NotNull IBlockState state) {
    return false;
  }

  Random rand = new Random();

  @Override
  public void breakBlock(World world, @NotNull BlockPos pos, @NotNull IBlockState state) {

    TileEntityTurretSentry sentry = (TileEntityTurretSentry) world.getTileEntity(pos);

    if (sentry != null) {
      for (int i = 0; i < sentry.inventory.getSlots(); ++i) {
        ItemStack itemStack = sentry.inventory.getStackInSlot(i);

        if (!itemStack.isEmpty()) {
          float oX = this.rand.nextFloat() * 0.8F + 0.1F;
          float oY = this.rand.nextFloat() * 0.8F + 0.1F;
          float oZ = this.rand.nextFloat() * 0.8F + 0.1F;

          while (itemStack.getCount() > 0) {
            int toDrop = this.rand.nextInt(21) + 10;

            if (toDrop > itemStack.getCount()) {
              toDrop = itemStack.getCount();
            }

            EntityItem entityitem =
                new EntityItem(
                    world,
                    pos.getX() + oX,
                    pos.getY() + oY,
                    pos.getZ() + oZ,
                    itemStack.splitStack(toDrop));

            float jump = 0.05F;
            entityitem.motionX = (float) this.rand.nextGaussian() * jump;
            entityitem.motionY = (float) this.rand.nextGaussian() * jump + 0.2F;
            entityitem.motionZ = (float) this.rand.nextGaussian() * jump;
            world.spawnEntity(entityitem);
          }
        }
      }

      InventoryHelper.dropInventoryItems(world, pos, world.getTileEntity(pos));
    }

    super.breakBlock(world, pos, state);
  }
}
