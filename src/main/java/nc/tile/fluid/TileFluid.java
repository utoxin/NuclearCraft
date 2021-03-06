package nc.tile.fluid;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import nc.ModCheck;
import nc.tile.NCTile;
import nc.tile.internal.fluid.FluidConnection;
import nc.tile.internal.fluid.FluidTileWrapper;
import nc.tile.internal.fluid.GasTileWrapper;
import nc.tile.internal.fluid.Tank;
import nc.tile.internal.fluid.TankSorption;
import nc.util.GasHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

public abstract class TileFluid extends NCTile implements ITileFluid {
	
	private final @Nonnull List<Tank> tanks;
	
	private @Nonnull FluidConnection[] fluidConnections;
	
	private @Nonnull FluidTileWrapper[] fluidSides;
	
	private @Nonnull GasTileWrapper gasWrapper;
	
	private boolean areTanksShared = false;
	private boolean emptyUnusableTankInputs = false;
	private boolean voidExcessFluidOutputs = false;
	
	public TileFluid(int capacity, @Nonnull TankSorption tankSorption, List<String> allowedFluidsList, @Nonnull FluidConnection[] fluidConnections) {
		this(Lists.newArrayList(capacity), Lists.newArrayList(capacity), Lists.newArrayList(tankSorption), Lists.<List<String>>newArrayList(allowedFluidsList), fluidConnections);
	}
	
	public TileFluid(@Nonnull List<Integer> capacity, @Nonnull List<TankSorption> tankSorptions, List<List<String>> allowedFluidsLists, @Nonnull FluidConnection[] fluidConnections) {
		this(capacity, capacity, tankSorptions, allowedFluidsLists, fluidConnections);
	}
	
	public TileFluid(int capacity, int maxTransfer, @Nonnull TankSorption tankSorption, List<String> allowedFluidsList, @Nonnull FluidConnection[] fluidConnections) {
		this(Lists.newArrayList(capacity), Lists.newArrayList(maxTransfer), Lists.newArrayList(tankSorption), Lists.<List<String>>newArrayList(allowedFluidsList), fluidConnections);
	}
	
	public TileFluid(@Nonnull List<Integer> capacity, @Nonnull List<Integer> maxTransfer, @Nonnull List<TankSorption> tankSorptions, List<List<String>> allowedFluidsLists, @Nonnull FluidConnection[] fluidConnections) {
		super();
		if (capacity.isEmpty()) {
			tanks = new ArrayList<Tank>();
		} else {
			List<Tank> tankList = new ArrayList<Tank>();
			for (int i = 0; i < capacity.size(); i++) {
				List<String> allowedFluidsList;
				if (allowedFluidsLists == null || allowedFluidsLists.size() <= i) allowedFluidsList = null;
				else allowedFluidsList = allowedFluidsLists.get(i);
				tankList.add(new Tank(capacity.get(i), tankSorptions.get(i), allowedFluidsList));
			}
			tanks = tankList;
		}
		this.fluidConnections = fluidConnections;
		fluidSides = ITileFluid.getDefaultFluidSides(this);
		gasWrapper = new GasTileWrapper(this);
	}
	
	@Override
	public @Nonnull List<Tank> getTanks() {
		return tanks;
	}
	
	@Override
	public @Nonnull FluidConnection[] getFluidConnections() {
		return fluidConnections;
	}
	
	@Override
	public void setFluidConnections(@Nonnull FluidConnection[] connections) {
		fluidConnections = connections;
	}

	@Override
	public @Nonnull FluidTileWrapper[] getFluidSides() {
		return fluidSides;
	}
	
	@Override
	public @Nonnull GasTileWrapper getGasWrapper() {
		return gasWrapper;
	}
	
	@Override
	public boolean getTanksShared() {
		return areTanksShared;
	}
	
	@Override
	public void setTanksShared(boolean shared) {
		areTanksShared = shared;
	}
	
	@Override
	public boolean getEmptyUnusableTankInputs() {
		return emptyUnusableTankInputs;
	}
	
	@Override
	public void setEmptyUnusableTankInputs(boolean emptyUnusableTankInputs) {
		this.emptyUnusableTankInputs = emptyUnusableTankInputs;
	}
	
	@Override
	public boolean getVoidExcessFluidOutputs() {
		return voidExcessFluidOutputs;
	}
	
	@Override
	public void setVoidExcessFluidOutputs(boolean voidExcessFluidOutputs) {
		this.voidExcessFluidOutputs = voidExcessFluidOutputs;
	}
	
	// NBT
	
	@Override
	public NBTTagCompound writeAll(NBTTagCompound nbt) {
		super.writeAll(nbt);
		writeTanks(nbt);
		writeFluidConnections(nbt);
		nbt.setBoolean("areTanksShared", areTanksShared);
		nbt.setBoolean("emptyUnusable", emptyUnusableTankInputs);
		nbt.setBoolean("voidExcessOutputs", voidExcessFluidOutputs);
		return nbt;
	}
		
	@Override
	public void readAll(NBTTagCompound nbt) {
		super.readAll(nbt);
		readTanks(nbt);
		readFluidConnections(nbt);
		setTanksShared(nbt.getBoolean("areTanksShared"));
		setEmptyUnusableTankInputs(nbt.getBoolean("emptyUnusable"));
		setVoidExcessFluidOutputs(nbt.getBoolean("voidExcessOutputs"));
	}
	
	// Capability
	
	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
		if (!getTanks().isEmpty() && hasFluidSideCapability(side)) {
			side = nonNullSide(side);
			if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return getFluidSide(side) != null;
			if (ModCheck.mekanismLoaded()) if (GasHelper.isGasCapability(capability)) return getGasWrapper() != null;
		}
		return super.hasCapability(capability, side);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
		if (!getTanks().isEmpty() && hasFluidSideCapability(side)) {
			side = nonNullSide(side);
			if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return (T) getFluidSide(side);
			if (ModCheck.mekanismLoaded()) if (GasHelper.isGasCapability(capability)) return (T) getGasWrapper();
		}
		return super.getCapability(capability, side);
	}
}
