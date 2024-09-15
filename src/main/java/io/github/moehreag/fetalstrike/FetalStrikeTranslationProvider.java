package io.github.moehreag.fetalstrike;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

public class FetalStrikeTranslationProvider extends FabricLanguageProvider {
	protected FetalStrikeTranslationProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
		super(dataOutput, registryLookup);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder translationBuilder) {
		translationBuilder.add(FetalStrikeDataGenerator.FETAL_STRIKE.location(), "Fetal Strike");
		translationBuilder.add("fetalstrike.advancement.description", "You murdered an innocent child");
		translationBuilder.add("fetalstrike.advancement.title", "YOU MONSTER");
		translationBuilder.add("enchantment.fetalstrike.fetalstrike", "Fetal Strike");
	}
}
