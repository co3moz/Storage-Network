package mrriegel.storagenetwork.cable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import mrriegel.storagenetwork.BlockConnectable;
import mrriegel.storagenetwork.ConfigHandler;
import mrriegel.storagenetwork.CreativeTab;
import mrriegel.storagenetwork.GuiHandler;
import mrriegel.storagenetwork.IConnectable;
import mrriegel.storagenetwork.ModBlocks;
import mrriegel.storagenetwork.ModItems;
import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.cable.TileKabel.Kind;
import mrriegel.storagenetwork.helper.InvHelper;
import mrriegel.storagenetwork.helper.Util;
import mrriegel.storagenetwork.master.TileMaster;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockKabel extends BlockConnectable {
  public static enum EnumConnectType implements IStringSerializable {
    CONNECT("connect"), STORAGE("storage"), NULL("null");
    String name;
    private EnumConnectType(String name) {
      this.name = name;
    }
    @Override
    public String getName() {
      return name;
    }
  }
  public BlockKabel() {
    super(Material.IRON);
    this.setHardness(1.4F);
    this.setCreativeTab(CreativeTab.tab1);
    // this.setDefaultState(blockState.getBaseState().withProperty(NORTH,
    // Connect.NULL).withProperty(SOUTH, Connect.NULL).withProperty(WEST,
    // Connect.NULL).withProperty(EAST, Connect.NULL).withProperty(UP,
    // Connect.NULL).withProperty(DOWN, Connect.NULL).withProperty(STRAIGHT,
    // false));
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
  @SideOnly(Side.CLIENT)
  public boolean isTranslucent(IBlockState state) {
    return true;
  }
  @Override
  public boolean isFullCube(IBlockState state) {
    return false;
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
    ItemStack heldItem = playerIn.getHeldItem(hand);
    if (!(worldIn.getTileEntity(pos) instanceof TileKabel))
      return false;
    if (worldIn.isRemote)
      return true;
    TileKabel tile = (TileKabel) worldIn.getTileEntity(pos);
    if (/* tile.getMaster() == null || */(heldItem != null && !heldItem.isEmpty() && (heldItem.getItem() == ModItems.duplicator)))
      return false;
    else if (tile.getKind() == Kind.exKabel || tile.getKind() == Kind.imKabel || tile.getKind() == Kind.storageKabel) {
      playerIn.openGui(StorageNetwork.instance, GuiHandler.CABLE, worldIn, pos.getX(), pos.getY(), pos.getZ());
      return true;
    }
    return false;
  }
  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
    setConnections(worldIn, pos, state, false);
  }
  // @Override
  // protected BlockStateContainer createBlockState() {
  // return new BlockStateContainer(this, new IProperty[] { NORTH, SOUTH,
  // EAST, WEST, UP, DOWN, STRAIGHT });
  // }
  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }
  boolean validInventory(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
    return InvHelper.hasItemHandler(worldIn, pos, side);
  }
  IBlockState getNewState(IBlockAccess world, BlockPos pos) {
    if (!(world.getTileEntity(pos) instanceof TileKabel))
      return world.getBlockState(pos);
    TileKabel tile = (TileKabel) world.getTileEntity(pos);
    EnumFacing face = null;
    BlockPos con = null;
    Map<EnumFacing, EnumConnectType> oldMap = tile.getConnects();
    Map<EnumFacing, EnumConnectType> newMap = Maps.newHashMap();
    EnumFacing stor = null;
    for (Entry<EnumFacing, EnumConnectType> e : oldMap.entrySet()) {
      if (e.getValue() == EnumConnectType.STORAGE) {
        stor = e.getKey();
        break;
      }
    }
    boolean storage = false;
    boolean first = false;
    if (stor != null && getConnect(world, pos, pos.offset(stor)) == EnumConnectType.STORAGE) {
      newMap.put(stor, EnumConnectType.STORAGE);
      storage = true;
      first = true;
    }
    for (EnumFacing f : EnumFacing.values()) {
      if (stor == f && first)
        continue;
      EnumConnectType neu = getConnect(world, pos, pos.offset(f));
      if (neu == EnumConnectType.STORAGE)
        if (!storage) {
        newMap.put(f, neu);
        storage = true;
        }
        else
          newMap.put(f, EnumConnectType.NULL);
      else
        newMap.put(f, neu);
    }
    tile.setConnects(newMap);
    if (tile.north == EnumConnectType.STORAGE) {
      face = EnumFacing.NORTH;
      con = pos.north();
    }
    else if (tile.south == EnumConnectType.STORAGE) {
      face = EnumFacing.SOUTH;
      con = pos.south();
    }
    else if (tile.east == EnumConnectType.STORAGE) {
      face = EnumFacing.EAST;
      con = pos.east();
    }
    else if (tile.west == EnumConnectType.STORAGE) {
      face = EnumFacing.WEST;
      con = pos.west();
    }
    else if (tile.down == EnumConnectType.STORAGE) {
      face = EnumFacing.DOWN;
      con = pos.down();
    }
    else if (tile.up == EnumConnectType.STORAGE) {
      face = EnumFacing.UP;
      con = pos.up();
    }
    tile.setInventoryFace(face);
    tile.setConnectedInventory(con);
    Map<EnumFacing, EnumConnectType> map = tile.getConnects();
    // return world.getBlockState(pos).withProperty(NORTH,
    // map.get(EnumFacing.NORTH)).withProperty(SOUTH,
    // map.get(EnumFacing.SOUTH)).withProperty(EAST,
    // map.get(EnumFacing.EAST)).withProperty(WEST,
    // map.get(EnumFacing.WEST)).withProperty(UP,
    // map.get(EnumFacing.UP)).withProperty(DOWN,
    // map.get(EnumFacing.DOWN)).withProperty(STRAIGHT, oo(tile));
    return world.getBlockState(pos);
  }
  @Override
  public void setConnections(World worldIn, BlockPos pos, IBlockState state, boolean refresh) {
    state = getNewState(worldIn, pos);
    super.setConnections(worldIn, pos, state, refresh);
    if (refresh)
      Util.updateTile(worldIn, pos);
    // worldIn.setBlockState(pos, state);
  }
  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
    try {
      IBlockState foo = getNewState(worldIn, pos);
      return foo;
    }
    catch (Exception e) {
      e.printStackTrace();
      return super.getActualState(state, worldIn, pos);
    }
  }
  private boolean oo(TileKabel tile) {
    boolean a = connected(tile.north) && connected(tile.south) && !connected(tile.west) && !connected(tile.east) && !connected(tile.up) && !connected(tile.down);
    boolean b = !connected(tile.north) && !connected(tile.south) && connected(tile.west) && connected(tile.east) && !connected(tile.up) && !connected(tile.down);
    boolean c = !connected(tile.north) && !connected(tile.south) && !connected(tile.west) && !connected(tile.east) && connected(tile.up) && connected(tile.down);
    return (a ^ b ^ c) && tile.getKind() == Kind.kabel;
  }
  private boolean connected(EnumConnectType c) {
    return c == EnumConnectType.STORAGE || c == EnumConnectType.CONNECT;
  }
  public static EnumFacing get(BlockPos a, BlockPos b) {
    if (a.up().equals(b))
      return EnumFacing.DOWN;
    if (a.down().equals(b))
      return EnumFacing.UP;
    if (a.west().equals(b))
      return EnumFacing.EAST;
    if (a.east().equals(b))
      return EnumFacing.WEST;
    if (a.north().equals(b))
      return EnumFacing.SOUTH;
    if (a.south().equals(b))
      return EnumFacing.NORTH;
    return null;
  }
  @Override
  public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean p_185477_7_) {
    if (!(worldIn.getTileEntity(pos) instanceof TileKabel))
      return;
    state = state.getActualState(worldIn, pos);
    TileKabel tile = (TileKabel) worldIn.getTileEntity(pos);
    if (tile != null && tile.getCover() != null) {
      if (tile.getCover() != Blocks.GLASS)
        addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_BLOCK_AABB);
      else if (ConfigHandler.untouchable)
        addCollisionBoxToList(pos, entityBox, collidingBoxes, NULL_AABB);
      return;
    }
    float f = 0.3125F;
    float f1 = 0.6875F;
    float f2 = 0.3125F;
    float f3 = 0.6875F;
    float f4 = 0.3125F;
    float f5 = 0.6875F;
    addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(f, f4, f2, f1, f5, f3));
    if (tile.north != EnumConnectType.NULL) {
      f2 = 0f;
      addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(f, f4, f2, f1, f5, f3));
    }
    if (tile.south != EnumConnectType.NULL) {
      f3 = 1f;
      addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(f, f4, f2, f1, f5, f3));
    }
    if (tile.west != EnumConnectType.NULL) {
      f = 0f;
      addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(f, f4, f2, f1, f5, f3));
    }
    if (tile.east != EnumConnectType.NULL) {
      f1 = 1f;
      addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(f, f4, f2, f1, f5, f3));
    }
    if (tile.down != EnumConnectType.NULL) {
      f4 = 0f;
      addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(f, f4, f2, f1, f5, f3));
    }
    if (tile.up != EnumConnectType.NULL) {
      f5 = 1f;
      addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(f, f4, f2, f1, f5, f3));
    }
  }
  @Override
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (!(source.getTileEntity(pos) instanceof TileKabel))
      return FULL_BLOCK_AABB;
    state = state.getActualState(source, pos);
    TileKabel tile = (TileKabel) source.getTileEntity(pos);
    float f = 0.3125F;
    float f1 = 0.6875F;
    float f2 = 0.3125F;
    float f3 = 0.6875F;
    float f4 = 0.3125F;
    float f5 = 0.6875F;
    if (tile == null)
      return new AxisAlignedBB(f, f4, f2, f1, f5, f3);
    if (tile != null && tile.getCover() != null && tile.getCover() != Blocks.GLASS) { return FULL_BLOCK_AABB; }
    AxisAlignedBB res = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    if (tile.north != EnumConnectType.NULL) {
      f2 = 0f;
    }
    if (tile.south != EnumConnectType.NULL) {
      f3 = 1f;
    }
    if (tile.west != EnumConnectType.NULL) {
      f = 0f;
    }
    if (tile.east != EnumConnectType.NULL) {
      f1 = 1f;
    }
    if (tile.down != EnumConnectType.NULL) {
      f4 = 0f;
    }
    if (tile.up != EnumConnectType.NULL) {
      f5 = 1f;
    }
    return new AxisAlignedBB(f, f4, f2, f1, f5, f3);
  }
  protected EnumConnectType getConnect(IBlockAccess worldIn, BlockPos orig, BlockPos pos) {
    Block block = worldIn.getBlockState(pos).getBlock();
    Block ori = worldIn.getBlockState(orig).getBlock();
    if (worldIn.getTileEntity(pos) instanceof IConnectable || worldIn.getTileEntity(pos) instanceof TileMaster)
      return EnumConnectType.CONNECT;
    if (ori == ModBlocks.kabel)
      return EnumConnectType.NULL;
    EnumFacing face = get(orig, pos);
    if (!validInventory(worldIn, pos, face))
      return EnumConnectType.NULL;
    return EnumConnectType.STORAGE;
  }
  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    TileEntity tileentity = worldIn.getTileEntity(pos);
    if (tileentity instanceof TileKabel) {
      TileKabel tile = (TileKabel) tileentity;
      for (int i = 0; i < tile.getUpgrades().size(); i++) {
        Util.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), tile.getUpgrades().get(i));
      }
      //			if (tile.getCover() != null)
      //				Util.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModBlocks.cover));
    }
    super.breakBlock(worldIn, pos, state);
  }
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileKabel();
  }
  public static class Item extends ItemBlock {
    public Item(Block block) {
      super(block);
    }
    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
      super.addInformation(stack, playerIn, tooltip, advanced);
      if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(ModBlocks.exKabel))
        tooltip.add(I18n.format("tooltip.storagenetwork.kabel_E"));
      else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(ModBlocks.imKabel))
        tooltip.add(I18n.format("tooltip.storagenetwork.kabel_I"));
      else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(ModBlocks.storageKabel))
        tooltip.add(I18n.format("tooltip.storagenetwork.kabel_S"));
      //			else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(ModBlocks.vacuumKabel))
      //				tooltip.add(I18n.format("tooltip.storagenetwork.kabel_V"));
      else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(ModBlocks.kabel))
        tooltip.add(I18n.format("tooltip.storagenetwork.kabel_L"));
      tooltip.add(I18n.format("tooltip.storagenetwork.networkNeeded"));
    }
  }
  public static class PropertyConnection extends PropertyEnum<EnumConnectType> {
    String name;
    public PropertyConnection(String name2) {
      super(name2, EnumConnectType.class, Lists.newArrayList(EnumConnectType.values()));
      this.name = name2;
    }
    public static PropertyConnection create(String name) {
      return new PropertyConnection(name);
    }
    @Override
    public String getName() {
      return name;
    }
  }
}
