package com.globbypotato.rockhounding_chemistry.machines.tileentity;

import java.util.ArrayList;

import com.globbypotato.rockhounding_chemistry.handlers.ModConfig;
import com.globbypotato.rockhounding_chemistry.machines.gui.GuiDepositionChamber;
import com.globbypotato.rockhounding_chemistry.machines.recipe.DepositionChamberRecipe;
import com.globbypotato.rockhounding_chemistry.machines.recipe.MachineRecipes;
import com.globbypotato.rockhounding_chemistry.utils.ToolUtils;
import com.globbypotato.rockhounding_core.machines.tileentity.MachineStackHandler;
import com.globbypotato.rockhounding_core.machines.tileentity.TemplateStackHandler;
import com.globbypotato.rockhounding_core.machines.tileentity.TileEntityMachineTank;
import com.globbypotato.rockhounding_core.machines.tileentity.WrappedItemHandler;
import com.globbypotato.rockhounding_core.machines.tileentity.WrappedItemHandler.WriteMode;
import com.globbypotato.rockhounding_core.utils.Utils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidHandlerConcatenate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityDepositionChamber extends TileEntityMachineTank{

	private ItemStackHandler template = new TemplateStackHandler(3);

	public static final int SOLVENT_SLOT = 1;
	public static final int CASING_SLOT = 2;
	public static final int INSULATION_SLOT = 3;
	public static final int SPEED_SLOT = 4;
	public int temperatureCount = 0;
	public int temperatureMax = 3000;
	public int pressureCount = 0;
	public int pressureMax = 32000;

	public int takenRF = 1000;

	public FluidTank inputTank;

	public static int totInput = 5;
	public static int totOutput = 1;

	public TileEntityDepositionChamber() {
		super(totInput, totOutput, 0);

		this.inputTank = new FluidTank(10000){
			@Override
			public boolean canFillFluidType(FluidStack fluid){
				return isActive() && hasSolvent(fluid) && isValidInterval() && isCorrectSolvent(fluid);
			}
			@Override
		    public boolean canDrain(){
		        return !isValidInterval() || (isValidInterval() && isWrongSolvent(TileEntityDepositionChamber.this.inputTank));
		    }
		};
		this.inputTank.setTileEntity(this);
		this.inputTank.setCanFill(true);

		this.input =  new MachineStackHandler(totInput,this){
			@SuppressWarnings("synthetic-access")
			@Override
			public void validateSlotIndex(int slot){
				if(TileEntityDepositionChamber.this.input.getSlots() < totInput){
					ItemStack[] stacksCloned = this.stacks;
					TileEntityDepositionChamber.this.input.setSize(totInput);
					for(int x = 0; x < stacksCloned.length; x++){
						this.stacks[x] = stacksCloned[x];
					}
				}
				super.validateSlotIndex(slot);
			}
			@Override
			public ItemStack insertItem(int slot, ItemStack insertingStack, boolean simulate){
				if(slot == INPUT_SLOT && isActive() && isValidInterval() && (hasRecipe(insertingStack) || isValidOredict(insertingStack))){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == CASING_SLOT && ToolUtils.hasUpgrade(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == INSULATION_SLOT && ToolUtils.hasInsulation(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == SOLVENT_SLOT && isValidInterval() && hasSolvent(FluidUtil.getFluidContained(insertingStack))){
					return super.insertItem(slot, insertingStack, simulate);
				}
				if(slot == SPEED_SLOT && ToolUtils.isValidSpeedUpgrade(insertingStack)){
					return super.insertItem(slot, insertingStack, simulate);
				}
				return insertingStack;
			}
		};
		this.automationInput = new WrappedItemHandler(this.input, WriteMode.IN);
		this.markDirtyClient();
	}



	//----------------------- SLOTS -----------------------
	public ItemStack speedSlot(){
		return this.input.getStackInSlot(SPEED_SLOT);
	}



	//----------------------- HANDLER -----------------------
	public ItemStackHandler getTemplate(){
		return this.template;
	}

	public int speedAnalyzer() {
		return ToolUtils.isValidSpeedUpgrade(speedSlot()) ? ModConfig.speedDeposition / ToolUtils.speedUpgrade(speedSlot()): ModConfig.speedDeposition;
	}

	public int getCookTimeMax() {
		return speedAnalyzer();
	}

	@Override
	public int getGUIHeight() {
		return GuiDepositionChamber.HEIGHT;
	}

	@Override
	public boolean hasRF(){
		return true;	
	}

	@Override
	public boolean canInduct() {
		return false;
	}



	//----------------------- CUSTOM ------------------------
	public boolean isValidInterval() {
		return this.recipeIndex >= 0 && this.recipeIndex <= MachineRecipes.depositionRecipes.size() - 1;
	}

	public DepositionChamberRecipe getRecipe(){
		return isValidInterval() ? MachineRecipes.depositionRecipes.get(this.recipeIndex) : null;
	}

	public static boolean isValidOredict(ItemStack stack) {
		if(stack != null){
			ArrayList<Integer> inputIDs = Utils.intArrayToList(OreDictionary.getOreIDs(stack));
			for(DepositionChamberRecipe recipe: MachineRecipes.depositionRecipes){
				ArrayList<Integer> recipeIDs = Utils.intArrayToList(OreDictionary.getOreIDs(recipe.getInput()));
				for(Integer ores: recipeIDs){
					String recipeName = OreDictionary.getOreName(ores);
					for(Integer inputs: inputIDs){
						String inputName = OreDictionary.getOreName(inputs);
						if(inputName.matches(recipeName)) return true;
					}
				}
			}
		}
		return false;
	}

	public boolean hasRecipe(ItemStack stack){
		return isValidInterval() && MachineRecipes.depositionRecipes.stream().anyMatch(
				recipe -> stack != null && recipe.getInput() != null && stack.isItemEqual(getRecipe().getInput()));
	}

	public boolean hasSolvent(FluidStack stack){
		return isValidInterval() && MachineRecipes.depositionRecipes.stream().anyMatch(
				recipe -> stack != null && recipe.getSolvent()!= null && stack.isFluidEqual(getRecipe().getSolvent()));
	}

	public boolean isWrongSolvent(FluidTank tank) {
		return isValidInterval() && tank.getFluid() != null && !tank.getFluid().isFluidEqual(getRecipe().getSolvent()) && tank.getFluidAmount() > 0;
	}

	public boolean isCorrectSolvent(FluidStack fluid) {
		return isValidInterval() && fluid.isFluidEqual(getRecipe().getSolvent());
	}

	public int getPressure() { 	  
		return this.pressureCount; 
	}

	public int getPressureMax() { 	  
		return this.pressureMax; 
	}

	public int getTemperature() { 	  
		return this.temperatureCount; 
	}

	public int getTemperatureMax() { 	  
		return this.temperatureMax; 
	}



	//----------------------- I/O -----------------------
	@Override
	public void readFromNBT(NBTTagCompound compound){
		super.readFromNBT(compound);
		this.recipeIndex = compound.getInteger("RecipeScan");
		this.pressureCount = compound.getInteger("PressureCount");
		this.temperatureCount = compound.getInteger("TemperatureCount");
		this.inputTank.readFromNBT(compound.getCompoundTag("InputTank"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound){
		super.writeToNBT(compound);
		compound.setInteger("RecipeScan", this.recipeIndex);
		compound.setInteger("PressureCount", this.pressureCount);
		compound.setInteger("TemperatureCount", this.temperatureCount);

		NBTTagCompound inputTankNBT = new NBTTagCompound();
		this.inputTank.writeToNBT(inputTankNBT);
		compound.setTag("InputTank", inputTankNBT);

		return compound;
	}

	@Override
	public FluidHandlerConcatenate getCombinedTank(){
		return new FluidHandlerConcatenate(this.inputTank);
	}



	//----------------------- PROCESS -----------------------
	@Override
	public void update(){
		acceptEnergy();
		if(!isValidInterval()){ this.recipeIndex = -1; }

		if(!this.worldObj.isRemote){
			emptyContainer(SOLVENT_SLOT, this.inputTank);

			if(isValidInterval()){
				handleParameters();

				if(canDeposit()){
					execute();
				}
			}else{
				handleRelaxing();
			}
			this.markDirtyClient();
		}
	}

	public boolean canDeposit(){
		return isActive()
			&& getRecipe() != null
			&& (hasRecipe(this.input.getStackInSlot(INPUT_SLOT)) || isValidOredict(this.input.getStackInSlot(INPUT_SLOT)))
			&& this.input.hasEnoughFluid(this.inputTank.getFluid(), getRecipe().getSolvent())
			&& this.getTemperature() >= getRecipe().getTemperature()
			&& this.getPressure() >= getRecipe().getPressure()
			&& this.output.canSetOrStack(this.output.getStackInSlot(OUTPUT_SLOT), getRecipe().getOutput());
	}

	private void execute() {
		this.cookTime++;
		if(this.cookTime >= getCookTimeMax()) {
			this.cookTime = 0; 
			handleOutput();
		}
	}

	private void handleOutput() {
		this.output.setOrStack(OUTPUT_SLOT, getRecipe().getOutput());
		this.input.drainOrClean(this.inputTank, getRecipe().getSolvent().amount, true);
		this.temperatureCount /= 3;
		this.pressureCount /= 3;
		this.input.decrementSlot(INPUT_SLOT);
	}

	private void handleRelaxing() {
		if(this.getTemperature() > tempStability() && this.rand.nextInt(stabilityDelta()) == 0){
			this.temperatureCount -= tempStability();
		}
		if(this.getPressure() > pressStability() && this.rand.nextInt(stabilityDelta()) == 0){
			this.pressureCount -= pressStability();
		}
	}

	private void handleParameters() {
		handleRelaxing();
		int temp = getRecipe().getTemperature();
		if(this.redstoneCount >= this.takenRF && this.getTemperature() < temp && temp < this.getTemperatureMax() - tempYeld() ){
			this.redstoneCount -= this.takenRF; 
			this.temperatureCount += tempYeld();
		}

		int press = getRecipe().getPressure();
		if(this.getRedstone() >= this.takenRF && this.getPressure() < press && press < this.getPressureMax() - pressYeld()){
			this.redstoneCount -= this.takenRF; 
			this.pressureCount += pressYeld();
		}
	}

	public int tempYeld(){
		return 10 * getUpgrade();
	}

	public int pressYeld(){
		return 30 * getUpgrade();
	}

	private int getUpgrade() {
		return ToolUtils.hasUpgrade(this.input.getStackInSlot(CASING_SLOT))? 10 : 1;
	}

	public int tempStability(){
		return 300 / getInsulation();
	}

	public int pressStability(){
		return 500 / getInsulation();
	}

	public int stabilityDelta(){
		return 100 + (10 * getInsulation());
	}

	private int getInsulation() {
		return ToolUtils.hasInsulation(this.input.getStackInSlot(INSULATION_SLOT)) ? 10 : 1;
	}

}