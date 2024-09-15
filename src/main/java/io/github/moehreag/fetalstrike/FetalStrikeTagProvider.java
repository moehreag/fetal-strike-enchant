package io.github.moehreag.fetalstrike;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.EnchantmentTags;

public class FetalStrikeTagProvider extends FabricTagProvider.EnchantmentTagProvider {
	public FetalStrikeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
		super(output, completableFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider wrapperLookup) {
		getOrCreateTagBuilder(EnchantmentTags.IN_ENCHANTING_TABLE)
				.add(FetalStrikeDataGenerator.FETAL_STRIKE);
	}
}
