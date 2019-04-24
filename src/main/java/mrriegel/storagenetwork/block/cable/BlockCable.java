package mrriegel.storagenetwork.block.cable;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import mrriegel.storagenetwork.CreativeTab;
import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.api.capability.IConnectable;
import mrriegel.storagenetwork.block.AbstractBlockConnectable;
import mrriegel.storagenetwork.block.master.TileMaster;
import mrriegel.storagenetwork.capabilities.StorageNetworkCapabilities;
import mrriegel.storagenetwork.gui.GuiHandler;
import mrriegel.storagenetwork.registry.ModBlocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockCable extends AbstractBlockConnectable {

  public static final UnlistedPropertyBlockNeighbors BLOCK_NEIGHBORS = new UnlistedPropertyBlockNeighbors();

  public BlockCable(String registryName) {
    super(Material.ROCK, registryName);
    this.setHardness(1.4F);
    this.setCreativeTab(CreativeTab.tab);
  }

  @Override
  protected BlockStateContainer createBlockState() {
    IProperty[] listedProperties = new IProperty[0];
    IUnlistedProperty[] unlistedProperties = new IUnlistedProperty[] { BLOCK_NEIGHBORS };
    return new ExtendedBlockState(this, listedProperties, unlistedProperties);
  }

  protected UnlistedPropertyBlockNeighbors.BlockNeighbors getBlockNeighbors(IBlockAccess world, BlockPos pos) {
    UnlistedPropertyBlockNeighbors.BlockNeighbors blockNeighbors = new UnlistedPropertyBlockNeighbors.BlockNeighbors();
    for (EnumFacing facing : EnumFacing.values()) {
      TileMaster tileMaster = getClientSideTileEntity(world, pos.offset(facing), TileMaster.class);
      IConnectable connectable = getClientSideCapability(world, pos.offset(facing), StorageNetworkCapabilities.CONNECTABLE_CAPABILITY, null);
      if (connectable == null && tileMaster == null) {
        continue;
      }
      blockNeighbors.setNeighborType(facing, UnlistedPropertyBlockNeighbors.EnumNeighborType.CABLE);
    }
    return blockNeighbors;
  }

  @Override
  public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
    IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;
    return extendedBlockState.withProperty(BLOCK_NEIGHBORS, getBlockNeighbors(world, pos));
  }

  @Nullable
  public <V> V getClientSideTileEntity(IBlockAccess world, BlockPos pos, Class<V> tileEntityClassOrInterface) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity == null) {
      return null;
    }
    if (!tileEntityClassOrInterface.isAssignableFrom(tileEntity.getClass())) {
      return null;
    }
    return (V) tileEntity;
  }

  @Nullable
  private <V> V getClientSideCapability(IBlockAccess world, BlockPos pos, Capability<V> capability, EnumFacing side) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity == null) {
      return null;
    }
    if (!tileEntity.hasCapability(capability, side)) {
      return null;
    }
    return tileEntity.getCapability(capability, side);
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileCable();
  }

  @Override
  public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
    return false;
  }

  @Override
  public boolean isOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess p_193383_1_, IBlockState p_193383_2_, BlockPos p_193383_3_, EnumFacing p_193383_4_) {
    return BlockFaceShape.MIDDLE_POLE_THIN;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public boolean isTranslucent(IBlockState state) {
    return true;
  }

  @Override
  public boolean canBeConnectedTo(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    return false;
  }

  @Override
  public boolean isFullCube(IBlockState state) {
    return false;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
    return layer == BlockRenderLayer.SOLID;
  }

  @Override
  public EnumBlockRenderType getRenderType(IBlockState state) {
    return EnumBlockRenderType.INVISIBLE;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    TileEntity tile = worldIn.getTileEntity(pos);
    if (!(tile instanceof TileCable)) {
      return false;
    }
    if (worldIn.isRemote) {
      return true;
    }
    // TODO: Move gui open actions to the block classes
    if (tile.getBlockType() == ModBlocks.exKabel) {
      playerIn.openGui(StorageNetwork.instance, GuiHandler.GuiIDs.EXPORT.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
      return true;
    }
    if (tile.getBlockType() == ModBlocks.imKabel) {
      playerIn.openGui(StorageNetwork.instance, GuiHandler.GuiIDs.IMPORT.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
      return true;
    }
    if (tile.getBlockType() == ModBlocks.storageKabel) {
      playerIn.openGui(StorageNetwork.instance, GuiHandler.GuiIDs.LINK.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
      return true;
    }
    if (tile.getBlockType() == ModBlocks.processKabel) {
      playerIn.openGui(StorageNetwork.instance, GuiHandler.GuiIDs.PROCESSING.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
      return true;
    }
    return false;
  }

  @Override
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean p_185477_7_) {
    addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_NONE);
    state = state.getActualState(world, pos);
    UnlistedPropertyBlockNeighbors.BlockNeighbors neighbors = getBlockNeighbors(world, pos);
    if (neighbors.north() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          AABB_SIDES.get(EnumFacing.NORTH).offset(pos));
    }
    if (neighbors.south() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          AABB_SIDES.get(EnumFacing.SOUTH).offset(pos));
    }
    if (neighbors.west() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          AABB_SIDES.get(EnumFacing.WEST).offset(pos));
    }
    if (neighbors.east() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          AABB_SIDES.get(EnumFacing.EAST).offset(pos));
    }
    if (neighbors.down() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          AABB_SIDES.get(EnumFacing.DOWN).offset(pos));
    }
    if (neighbors.up() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          AABB_SIDES.get(EnumFacing.UP).offset(pos));
    }
  }

  private static final double SML = 0.37D;
  private static final double LRG = 1.0D - SML;
  public static final AxisAlignedBB AABB_NONE = new AxisAlignedBB(SML, SML, SML, LRG, LRG, LRG);
  public static final Map<EnumFacing, AxisAlignedBB> AABB_SIDES = Maps.newEnumMap(
      new ImmutableMap.Builder<EnumFacing, AxisAlignedBB>()
          .put(EnumFacing.DOWN, new AxisAlignedBB(SML, 0.0D, SML, LRG, SML, LRG))
          .put(EnumFacing.UP, new AxisAlignedBB(SML, LRG, SML, LRG, 1.0D, LRG))
          .put(EnumFacing.NORTH, new AxisAlignedBB(SML, SML, 0.0D, LRG, LRG, SML))
          .put(EnumFacing.SOUTH, new AxisAlignedBB(SML, SML, LRG, LRG, LRG, 1.0D))
          .put(EnumFacing.WEST, new AxisAlignedBB(0.0D, SML, SML, SML, LRG, LRG))
          .put(EnumFacing.EAST, new AxisAlignedBB(LRG, SML, SML, 1.0D, LRG, LRG))
          .build());

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
    AxisAlignedBB box = AABB_NONE.offset(pos);
    state = state.getActualState(world, pos);
    UnlistedPropertyBlockNeighbors.BlockNeighbors neighbors = getBlockNeighbors(world, pos);
    if (neighbors.north() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      box = box.union(AABB_SIDES.get(EnumFacing.NORTH).offset(pos));
    }
    if (neighbors.south() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      box = box.union(AABB_SIDES.get(EnumFacing.SOUTH).offset(pos));
    }
    if (neighbors.west() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      box = box.union(AABB_SIDES.get(EnumFacing.WEST).offset(pos));
    }
    if (neighbors.east() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      box = box.union(AABB_SIDES.get(EnumFacing.EAST).offset(pos));
    }
    if (neighbors.down() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      box = box.union(AABB_SIDES.get(EnumFacing.DOWN).offset(pos));
    }
    if (neighbors.up() != UnlistedPropertyBlockNeighbors.EnumNeighborType.NONE) {
      box = box.union(AABB_SIDES.get(EnumFacing.UP).offset(pos));
    }
    return box;
  }

  @Override
  public void addInformation(ItemStack stack, World playerIn, List<String> tooltip, ITooltipFlag advanced) {
    super.addInformation(stack, playerIn, tooltip, advanced);
    if (stack.getItem() == Item.getItemFromBlock(ModBlocks.exKabel))
      tooltip.add(I18n.format("tooltip.storagenetwork.kabel_E"));
    else if (stack.getItem() == Item.getItemFromBlock(ModBlocks.imKabel))
      tooltip.add(I18n.format("tooltip.storagenetwork.kabel_I"));
    else if (stack.getItem() == Item.getItemFromBlock(ModBlocks.storageKabel))
      tooltip.add(I18n.format("tooltip.storagenetwork.kabel_S"));
    else if (stack.getItem() == Item.getItemFromBlock(ModBlocks.kabel))
      tooltip.add(I18n.format("tooltip.storagenetwork.kabel_L"));
    else if (stack.getItem() == Item.getItemFromBlock(ModBlocks.processKabel))
      tooltip.add(I18n.format("tooltip.storagenetwork.kabel_P"));
  }
}
