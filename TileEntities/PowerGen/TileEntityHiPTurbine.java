/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.ReactorCraft.TileEntities.PowerGen;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;
import Reika.DragonAPI.Instantiable.RelativePositionList;
import Reika.DragonAPI.Instantiable.Data.BlockArray;
import Reika.DragonAPI.Libraries.ReikaDirectionHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.Registry.ReikaParticleHelper;
import Reika.DragonAPI.ModInteract.BCMachineHandler;
import Reika.ReactorCraft.Registry.ReactorTiles;
import Reika.ReactorCraft.Registry.WorkingFluid;
import Reika.ReactorCraft.TileEntities.Fission.TileEntityReactorBoiler;
import Reika.RotaryCraft.Registry.ConfigRegistry;
import Reika.RotaryCraft.Registry.MachineRegistry;
import Reika.RotaryCraft.TileEntities.Storage.TileEntityReservoir;

public class TileEntityHiPTurbine extends TileEntityTurbineCore {

	public static final int GEN_OMEGA = 131072;
	public static final int TORQUE_CAP = 65536;
	private WorkingFluid fluid = WorkingFluid.EMPTY;

	private RelativePositionList getInjectors() {
		RelativePositionList injectors = new RelativePositionList();
		ForgeDirection dir = this.getSteamMovement();
		if (dir.offsetX == 0) {
			injectors.addPosition(1, 1, 0);
			injectors.addPosition(0, 1, 0);
			injectors.addPosition(-1, 1, 0);

			injectors.addPosition(1, 0, 0);

			injectors.addPosition(-1, 0, 0);

			injectors.addPosition(1, -1, 0);
			injectors.addPosition(0, -1, 0);
			injectors.addPosition(-1, -1, 0);
		}
		else if (dir.offsetZ == 0) {
			injectors.addPosition(0, 1, 1);
			injectors.addPosition(0, 1, 0);
			injectors.addPosition(0, 1, -1);

			injectors.addPosition(0, 0, 1);

			injectors.addPosition(0, 0, -1);

			injectors.addPosition(0, -1, 1);
			injectors.addPosition(0, -1, 0);
			injectors.addPosition(0, -1, -1);
		}
		return injectors;
	}

	@Override
	public boolean needsMultiblock() {
		return false;
	}

	@Override
	public int getMaxTorque() {
		return 65536;
	}

	@Override
	public int getMaxSpeed() {
		return 131072;
	}

	@Override
	protected int getMaxStage() {
		return 6;
	}

	@Override
	protected double getRadius() {
		return 1.5+this.getStage()/2;
	}

	@Override
	protected void copyDataFrom(TileEntityTurbineCore tile) {
		super.copyDataFrom(tile);
		fluid = ((TileEntityHiPTurbine)tile).fluid;
	}

	@Override
	protected void dumpSteam(World world, int x, int y, int z, int meta) {
		int stage = this.getStage();
		if (stage == this.getNumberStagesTotal()-1) {
			ForgeDirection s = this.getSteamMovement();
			ForgeDirection dir = ReikaDirectionHelper.getLeftBy90(s);
			int th = (int)(this.getRadius());
			int ty = y-th;
			for (int i = -th; i <= th; i++) {
				int tx = x+dir.offsetX*i+s.offsetX;
				int tz = z+dir.offsetZ*i+s.offsetZ;
				MachineRegistry m = MachineRegistry.getMachine(world, tx, ty, tz);
				FluidStack fs = new FluidStack(fluid.getLowPressureFluid(), TileEntityReactorBoiler.WATER_PER_STEAM);
				if (m == MachineRegistry.RESERVOIR) {
					TileEntity te = this.getTileEntity(tx, ty, tz);
					((TileEntityReservoir)te).addLiquid(fs.amount, fs.getFluid());
				}
				else if (world.getBlockId(tx, ty, tz) == BCMachineHandler.getInstance().tankID) {
					TileEntity te = this.getTileEntity(tx, ty, tz);
					((IFluidHandler)te).fill(ForgeDirection.UP, fs, true);
				}
				int py = ty+1+rand.nextInt(th*2);
				if (ReikaMathLibrary.py3d(dir.offsetX*i, py-y, dir.offsetZ*i) < th)
					ReikaParticleHelper.DRIPWATER.spawnAroundBlock(world, x+dir.offsetX*i, py, z+dir.offsetZ*i, 5);
			}
			int n = ConfigRegistry.SPRINKLER.getValue()*12;
			double ax = s.offsetX > 0 ? 1.2 : -0.2;
			double az = s.offsetZ > 0 ? 1.2 : -0.2;
			int d = -s.offsetX+s.offsetZ;
			for (int i = 0; i < n; i++) {
				double px = x+(-th+rand.nextDouble()*th*2+d)*dir.offsetX;
				double pz = z+(-th+rand.nextDouble()*th*2+d)*dir.offsetZ;
				ReikaParticleHelper.RAIN.spawnAt(world, px+ax, ty+1+rand.nextInt(th*2), pz+az);
			}
		}
	}

	@Override
	protected double getEfficiency() {
		switch(this.getNumberStagesTotal()) {
		case 1:
			return 0.0125;
		case 2:
			return 0.025;
		case 3:
			return 0.075;
		case 4:
			return 0.125;
		case 5:
			return 0.25;
		case 6:
			return 0.5;
		case 7:
			return 1;
		default:
			return 0;
		}
	}

	@Override
	public int getIndex() {
		return ReactorTiles.BIGTURBINE.ordinal();
	}

	@Override
	protected double getAnimationSpeed() {
		return 0.5F;
	}

	@Override
	protected void intakeLubricant(World world, int x, int y, int z, int meta) {
		ForgeDirection dir = this.getSteamMovement().getOpposite();
		int dx = x+dir.offsetX;
		int dy = y+dir.offsetY;
		int dz = z+dir.offsetZ;

		if (this.getStage() == 0) {
			RelativePositionList li = this.getInjectors();
			BlockArray pos = li.getPositionsRelativeTo(dx, dy, dz);
			for (int i = 0; i < pos.getSize(); i++) {
				int[] xyz = pos.getNthBlock(i);
				int sx = xyz[0];
				int sy = xyz[1];
				int sz = xyz[2];
				TileEntity tile = world.getBlockTileEntity(sx, sy, sz);
				if (tile instanceof TileEntitySteamInjector) {
					TileEntitySteamInjector te = (TileEntitySteamInjector)tile;
					int lube = te.getLubricant();
					int rem = Math.min(lube, tank.getRemainingSpace());
					if (rem > 0) {
						te.remove(rem);
						tank.addLiquid(rem, FluidRegistry.getFluid("lubricant"));
					}
				}
			}
		}
	}

	@Override
	protected boolean intakeSteam(World world, int x, int y, int z, int meta) {
		ForgeDirection dir = this.getSteamMovement().getOpposite();
		int dx = x+dir.offsetX;
		int dy = y+dir.offsetY;
		int dz = z+dir.offsetZ;

		boolean flag = false;

		ReactorTiles r = ReactorTiles.getTE(world, dx, dy, dz);
		if (r == ReactorTiles.STEAMLINE) {
			TileEntitySteamLine te = (TileEntitySteamLine)this.getAdjacentTileEntity(dir);
			int s = te.getSteam();
			if (s > 8 && this.canTakeIn(te.getWorkingFluid())) {
				int rm = s/8+1;
				steam += rm;
				fluid = te.getWorkingFluid();
				te.removeSteam(rm);
				flag = s > rm+32;
			}
		}
		else if (r == this.getMachine()) {
			TileEntityHiPTurbine te = (TileEntityHiPTurbine)this.getAdjacentTileEntity(dir);
			fluid = te.fluid;
		}

		if (steam == 0) {
			fluid = WorkingFluid.EMPTY;
		}

		return flag;
	}

	@Override
	protected int getConsumedLubricant() {
		return 5;
	}

	private boolean canTakeIn(WorkingFluid f) {
		return fluid == WorkingFluid.EMPTY || f == fluid;
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT)
	{
		super.readSyncTag(NBT);

		fluid = WorkingFluid.getFromNBT(NBT);
	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT)
	{
		super.writeSyncTag(NBT);

		fluid.saveToNBT(NBT);
	}

	@Override
	protected float getTorqueFactor() {
		return fluid.efficiency > 1 ? 1+(fluid.efficiency-1)*0.25F : fluid.efficiency;
	}

}
