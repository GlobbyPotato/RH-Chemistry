package com.globbypotato.rockhounding_chemistry.blocks;

import javax.annotation.Nullable;

import com.globbypotato.rockhounding_chemistry.Rhchemistry;
import com.globbypotato.rockhounding_chemistry.machines.tileentity.TileEntityInv;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class BaseTileBlock extends BaseBlock implements ITileEntityProvider{
	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	final Class<? extends TileEntity> tileClass;
	public int guiID;


	public BaseTileBlock(String name, Material material, Class<? extends TileEntity> tileClass, int guiID) {
		super(name);
		this.tileClass = tileClass;
		this.guiID = guiID;
		String tileName = name.substring(0,1).toUpperCase() + name.substring(1);
		GameRegistry.registerTileEntity(tileClass, tileName);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@SideOnly(Side.CLIENT)
	public void initModel() {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}

	public void setDefaultFacing(World worldIn, BlockPos pos, IBlockState state){
		if (!worldIn.isRemote){
			EnumFacing enumfacing = (EnumFacing)state.getValue(FACING);
			worldIn.setBlockState(pos, state.withProperty(FACING, enumfacing), 2);
		}
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state){
		this.setDefaultFacing(worldIn, pos, state);
	}
	
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		try {
			return tileClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state){
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileEntityInv){
			if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,null)){
				IItemHandler inventory = ((TileEntityInv) te).getInventory();
				int slots = inventory.getSlots();
				for(int i=0;i<slots; i++){
					if(inventory.getStackInSlot(i) != null){
						world.spawnEntityInWorld(new EntityItem(world,pos.getX(),pos.getY(),pos.getZ(),inventory.getStackInSlot(i)));
					}
				}
			}
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (!world.isRemote)
		{
			player.openGui(Rhchemistry.instance, guiID, world, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer){
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack){
        worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing()), 2);
    }
    
    @Override
	public boolean hasComparatorInputOverride(IBlockState state){
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos){
		return Container.calcRedstone(worldIn.getTileEntity(pos));
	}

    public EnumBlockRenderType getRenderType(IBlockState state){
        return EnumBlockRenderType.MODEL;
    }
    
    @Override
    public IBlockState getStateFromMeta(int meta){
        EnumFacing enumfacing = EnumFacing.getFront(meta);
        if (enumfacing.getAxis() == EnumFacing.Axis.Y){
            enumfacing = EnumFacing.NORTH;
        }
        return this.getDefaultState().withProperty(FACING, enumfacing);
    }

    @Override
    public int getMetaFromState(IBlockState state){
        return ((EnumFacing)state.getValue(FACING)).getIndex();
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot){
        return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn){
        return state.withRotation(mirrorIn.toRotation((EnumFacing)state.getValue(FACING)));
    }

    @Override
    public BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, new IProperty[] {FACING});
    }

}