package nc.util;

import nc.capability.radiation.IDefaultRadiationResistance;
import nc.capability.radiation.IEntityRads;
import nc.capability.radiation.IRadiation;
import nc.capability.radiation.IRadiationSource;
import nc.config.NCConfig;
import nc.radiation.RadBiomes;
import nc.tile.radiation.ITileRadiationEnvironment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class RadiationHelper {
	
	public static void addToChunkBuffer(IRadiationSource chunkRadiation, double addedRadiation) {
		chunkRadiation.setRadiationBuffer(chunkRadiation.getRadiationBuffer() + addedRadiation);
	}
	
	/** Only use for radiation leaks, etc. */
	public static void addToChunkRadiation(IRadiationSource chunkRadiation, double addedRadiation) {
		chunkRadiation.setRadiationLevel(chunkRadiation.getRadiationLevel() + addedRadiation);
	}
	
	// Block -> ChunkBuffer
	
	public static void addToChunkBuffer(Chunk chunk, double addedRadiation) {
		if (chunk == null || !chunk.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return;
		IRadiationSource chunkRadiation = chunk.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (chunkRadiation == null) return;
		addToChunkBuffer(chunkRadiation, addedRadiation);
	}
	
	/** Only use for radiation leaks, etc. */
	public static void addToChunkRadiation(Chunk chunk, double addedRadiation) {
		if (chunk == null || !chunk.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return;
		IRadiationSource chunkRadiation = chunk.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (chunkRadiation == null) return;
		addToChunkRadiation(chunkRadiation, addedRadiation);
	}
	
	// ITileRadiationEnvironment -> ChunkBuffer
	
	public static void addFractionToChunkBuffer(Chunk chunk, ITileRadiationEnvironment tile) {
		if (chunk == null || !chunk.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return;
		IRadiationSource chunkRadiation = chunk.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (chunkRadiation == null) return;
		addToChunkBuffer(chunkRadiation, tile.getChunkBufferContributionFraction()*tile.getCurrentChunkBuffer());
	}
	
	// ItemStack -> ChunkBuffer
	
	public static void transferRadiationFromStackToChunkBuffer(ItemStack stack, Chunk chunk) {
		if (chunk == null || !stack.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null) || !chunk.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return;
		IRadiationSource stackRadiation = stack.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (stackRadiation == null) return;
		IRadiationSource chunkRadiation = chunk.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (chunkRadiation == null) return;
		addToChunkBuffer(chunkRadiation, stackRadiation.getRadiationLevel()*stack.getCount());
	}
	
	// Source -> ChunkBuffer
	
	public static void transferRadiationFromSourceToChunkBuffer(ICapabilityProvider provider, Chunk chunk) {
		if (chunk == null || !provider.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null) || !chunk.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return;
		IRadiationSource sourceRadiation = provider.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (sourceRadiation == null) return;
		IRadiationSource chunkRadiation = chunk.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (chunkRadiation == null) return;
		addToChunkBuffer(chunkRadiation, sourceRadiation.getRadiationLevel());
	}
	
	// Inventory -> ChunkBuffer
	
	public static void transferRadsFromInventoryToChunkBuffer(InventoryPlayer inventory, Chunk chunk) {
		if (!NCConfig.radiation_hardcore_stacks) return;
		for (ItemStack stack : inventory.mainInventory) {
			if (!stack.isEmpty()) transferRadiationFromSourceToChunkBuffer(stack, chunk);
		}
		for (ItemStack stack : inventory.armorInventory) {
			if (!stack.isEmpty()) transferRadiationFromSourceToChunkBuffer(stack, chunk);
		}
		for (ItemStack stack : inventory.offHandInventory) {
			if (!stack.isEmpty()) transferRadiationFromSourceToChunkBuffer(stack, chunk);
		}
	}
	
	// Chunk Set Previous Radiation and Spread
	
	public static void spreadRadiationFromChunk(Chunk sourceChunk, Chunk targetChunk) {
		if (sourceChunk == null || !sourceChunk.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return;
		IRadiationSource sourceChunkRadiation = sourceChunk.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (sourceChunkRadiation == null) return;
		
		if (targetChunk != null && targetChunk.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) {
			IRadiationSource targetChunkRadiation = targetChunk.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
			if (targetChunkRadiation != null) {
				if (!sourceChunkRadiation.isRadiationNegligible()) {
					if (targetChunkRadiation.getRadiationLevel() == 0D || sourceChunkRadiation.getRadiationLevel()/targetChunkRadiation.getRadiationLevel() > 1.5D) {
						double radiationSpread = (sourceChunkRadiation.getRadiationLevel() - targetChunkRadiation.getRadiationLevel())*NCConfig.radiation_spread_rate;
						sourceChunkRadiation.setRadiationLevel(sourceChunkRadiation.getRadiationLevel() - radiationSpread);
						targetChunkRadiation.setRadiationLevel(targetChunkRadiation.getRadiationLevel() + radiationSpread);
					}
				}
			}
		}
		
		if (sourceChunkRadiation.isRadiationNegligible()) sourceChunkRadiation.setRadiationLevel(0D);
	}
	
	// Player Radiation Resistance
	
	public static double addRadsToPlayer(EntityPlayer player, IEntityRads playerRads, double rawRadiation, int updateRate) {
		double resistance = playerRads.getRadiationResistance() + getPlayerArmorRadResistance(player);
		
		double addedRadiation = rawRadiation <= 0D ? 0D : NCMath.square(rawRadiation)/(rawRadiation + resistance);
		playerRads.setTotalRads(playerRads.getTotalRads() + addedRadiation*updateRate, true);
		return addedRadiation;
	}
	
	public static double getPlayerArmorRadResistance(EntityPlayer player) {
		double resistance = 0D;
		resistance += getArmorInventoryRadResistance(player);
		return resistance;
	}
	
	private static double getArmorInventoryRadResistance(Entity entity) {
		if (entity == null) return 0D;
		double resistance = 0D;
		for (ItemStack armor : entity.getArmorInventoryList()) {
			if (armor.hasCapability(IDefaultRadiationResistance.CAPABILITY_DEFAULT_RADIATION_RESISTANCE, null)) {
				IDefaultRadiationResistance armorResistance = armor.getCapability(IDefaultRadiationResistance.CAPABILITY_DEFAULT_RADIATION_RESISTANCE, null);
				if (armorResistance != null) resistance += armorResistance.getRadiationResistance();
			}
			if (armor.hasTagCompound()) if (armor.getTagCompound().hasKey("ncRadiationResistance")) {
				resistance += armor.getTagCompound().getDouble("ncRadiationResistance");
			}
		}
		return resistance;
	}
	
	// Entity Radiation Resistance
	
	public static double addRadsToEntity(EntityLiving entityLiving, IEntityRads entityRads, double rawRadiation, int updateRate) {
		double resistance = entityRads.getRadiationResistance() + getEntityArmorRadResistance(entityLiving);
		
		double addedRadiation = rawRadiation <= 0D ? 0D : NCMath.square(rawRadiation)/(rawRadiation + resistance);
		entityRads.setTotalRads(entityRads.getTotalRads() + addedRadiation*updateRate, true);
		return addedRadiation;
	}
	
	public static double getEntityArmorRadResistance(EntityLiving entityLiving) {
		double resistance = 0D;
		resistance += getArmorInventoryRadResistance(entityLiving);
		if (NCConfig.radiation_horse_armor_public && entityLiving instanceof EntityHorse) resistance += getHorseArmorRadResistance((EntityHorse)entityLiving);
		return resistance;
	}
	
	private static double getHorseArmorRadResistance(EntityHorse horse) {
		if (horse == null) return 0D;
		double resistance = 0D;
		
		NBTTagCompound compound = new NBTTagCompound();
		horse.writeEntityToNBT(compound);
		ItemStack armor = new ItemStack(compound.getCompoundTag("ArmorItem"));
		if (armor != null && ArmorHelper.isHorseArmor(armor.getItem())) {
			if (armor.hasCapability(IDefaultRadiationResistance.CAPABILITY_DEFAULT_RADIATION_RESISTANCE, null)) {
				IDefaultRadiationResistance armorResistance = armor.getCapability(IDefaultRadiationResistance.CAPABILITY_DEFAULT_RADIATION_RESISTANCE, null);
				if (armorResistance != null) resistance += armorResistance.getRadiationResistance();
			}
			if (armor.hasTagCompound()) if (armor.getTagCompound().hasKey("ncRadiationResistance")) {
				resistance += armor.getTagCompound().getDouble("ncRadiationResistance");
			}
		}
		return resistance;
	}
	
	// Inventory -> Player
	
	public static double transferRadsFromInventoryToPlayer(EntityPlayer player, IEntityRads playerRads, Chunk chunk, int updateRate) {
		double radiationLevel = 0D;
		InventoryPlayer inventory = player.inventory;
		for (ItemStack stack : inventory.mainInventory) {
			radiationLevel += transferRadsFromStackToPlayer(stack, player, playerRads, updateRate);
		}
		for (ItemStack stack : inventory.armorInventory) {
			radiationLevel += transferRadsFromStackToPlayer(stack, player, playerRads, updateRate);
		}
		for (ItemStack stack : inventory.offHandInventory) {
			radiationLevel += transferRadsFromStackToPlayer(stack, player, playerRads, updateRate);
		}
		return radiationLevel;
	}
	
	private static double transferRadsFromStackToPlayer(ItemStack stack, EntityPlayer player, IEntityRads playerRads, int updateRate) {
		if (stack.isEmpty() || !stack.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return 0D;
		IRadiationSource stackRadiation = stack.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (stackRadiation == null) return 0D;
		return addRadsToPlayer(player, playerRads, stackRadiation.getRadiationLevel()*stack.getCount(), updateRate);
	}
	
	// Source -> Player
	
	public static double transferRadsToPlayer(ICapabilityProvider provider, EntityPlayer player, IEntityRads playerRads, int updateRate) {
		if (provider == null || !provider.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return 0D;
		IRadiationSource sourceRadiation = provider.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (sourceRadiation == null) return 0D;
		return addRadsToPlayer(player, playerRads, sourceRadiation.getRadiationLevel(), updateRate);
	}
	
	// Biome -> Player
	
	public static double transferBackgroundRadsToPlayer(Biome biome, EntityPlayer player, IEntityRads playerRads, int updateRate) {
		Double biomeRadiation = RadBiomes.BIOME_MAP.get(biome);
		if (biomeRadiation == null) return 0D;
		return addRadsToPlayer(player, playerRads, biomeRadiation, updateRate);
	}
	
	// Source -> Entity
	
	public static void transferRadsFromSourceToEntity(ICapabilityProvider provider, EntityLiving entityLiving, IEntityRads entityRads, int updateRate) {
		if (provider == null || !provider.hasCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null)) return;
		IRadiationSource sourceRadiation = provider.getCapability(IRadiationSource.CAPABILITY_RADIATION_SOURCE, null);
		if (sourceRadiation == null) return;
		entityRads.setRadiationLevel(addRadsToEntity(entityLiving, entityRads, sourceRadiation.getRadiationLevel(), updateRate));
	}
	
	// Biome -> Entity
	
	public static void transferBackgroundRadsToEntity(Biome biome, EntityLiving entityLiving, IEntityRads entityRads, int updateRate) {
		Double biomeRadiation = RadBiomes.BIOME_MAP.get(biome);
		if (biomeRadiation != null) entityRads.setRadiationLevel(addRadsToEntity(entityLiving, entityRads, biomeRadiation, updateRate));
	}
	
	// Entity Symptoms
	
	public static void applySymptoms(EntityLivingBase entity, IEntityRads entityRads, int updateRate) {
		int radPercentage = entityRads.getRadsPercentage();
		if (radPercentage < 40) return;
		else if (radPercentage < 55) {
			entity.addPotionEffect(PotionHelper.newEffect(18, 1, updateRate + 1));
		}
		else if (radPercentage < 70) {
			entity.addPotionEffect(PotionHelper.newEffect(18, 1, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(4, 1, updateRate + 1));
		}
		else if (radPercentage < 80) {
			entity.addPotionEffect(PotionHelper.newEffect(18, 2, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(4, 1, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(17, 1, updateRate + 1));
		}
		else if (radPercentage < 90) {
			entity.addPotionEffect(PotionHelper.newEffect(18, 2, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(4, 2, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(17, 1, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(19, 1, updateRate + 1));
		}
		else {
			entity.addPotionEffect(PotionHelper.newEffect(18, 3, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(4, 3, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(17, 2, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(19, 1, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(20, 1, updateRate + 1));
		}
	}
	
	public static void applyMobBuffs(EntityLiving entity, IEntityRads entityRads, int updateRate) {
		int radPercentage = entityRads.getRadsPercentage();
		if (radPercentage < 40) return;
		else if (radPercentage < 55) {
			entity.addPotionEffect(PotionHelper.newEffect(1, 1, updateRate + 1));
		}
		else if (radPercentage < 70) {
			entity.addPotionEffect(PotionHelper.newEffect(1, 1, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(8, 1, updateRate + 1));
		}
		else if (radPercentage < 80) {
			entity.addPotionEffect(PotionHelper.newEffect(1, 2, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(8, 1, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(11, 1, updateRate + 1));
		}
		else if (radPercentage < 90) {
			entity.addPotionEffect(PotionHelper.newEffect(1, 2, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(8, 2, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(11, 1, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(22, 1, updateRate + 1));
		}
		else {
			entity.addPotionEffect(PotionHelper.newEffect(1, 3, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(8, 3, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(11, 2, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(22, 1, updateRate + 1));
			entity.addPotionEffect(PotionHelper.newEffect(10, 1, updateRate + 1));
		}
	}
	
	// Text Colours
	
	public static TextFormatting getRadsTextColor(IEntityRads playerRads) {
		int radsPercent = playerRads.getRadsPercentage();
		return radsPercent < 30 ? TextFormatting.WHITE : (radsPercent < 55 ? TextFormatting.YELLOW : (radsPercent < 80 ? TextFormatting.RED : TextFormatting.DARK_RED));
	}
	
	public static TextFormatting getRadiationTextColor(double radiation) {
		if (radiation < 0.000000001D) return TextFormatting.WHITE;
		return radiation < 0.01D ? TextFormatting.YELLOW : (radiation < 1D ? TextFormatting.RED : TextFormatting.DARK_RED);
	}
	
	public static TextFormatting getRadiationTextColor(IRadiation irradiated) {
		return getRadiationTextColor(irradiated.getRadiationLevel());
	}
}
