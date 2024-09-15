package io.github.moehreag.fetalstrike;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.MultiplyValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import org.jetbrains.annotations.NotNull;

public class FetalStrikeDataGenerator implements DataGeneratorEntrypoint {
	public static final ResourceKey<Enchantment> FETAL_STRIKE = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("fetalstrike", "fetalstrike"));

	public static void bootstrapEnchant(BootstrapContext<Enchantment> bootstrapContext) {
		var items = bootstrapContext.lookup(Registries.ITEM);
		bootstrapContext.register(FETAL_STRIKE,
				Enchantment.enchantment(Enchantment.definition(
								items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
								4, 1,
								Enchantment.constantCost(20), Enchantment.constantCost(5), 4, EquipmentSlotGroup.MAINHAND))
						.withEffect(EnchantmentEffectComponents.DAMAGE, new MultiplyValue(LevelBasedValue.perLevel(100)),
								LootItemEntityPropertyCondition.hasProperties(
										LootContext.EntityTarget.THIS,
										EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true)))
						)
						.build(FETAL_STRIKE.location()));
	}

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();


		pack.addProvider(FetalStrikeTagProvider::new);
		pack.addProvider(FetalStrikeTranslationProvider::new);
		pack.addProvider(AdvancementProvider::new);
		pack.addProvider((output, registries) -> new DataProvider() {

			@Override
			public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
				return registries.thenCompose(provider -> {
					DynamicOps<JsonElement> dynamicOps = provider.createSerializationContext(JsonOps.INSTANCE);
					return CompletableFuture.allOf(
							RegistryDataLoader.WORLDGEN_REGISTRIES
									.stream()
									.flatMap(registryData -> this.dumpRegistryCap(cachedOutput, provider, dynamicOps, registryData).stream())
									.toArray(CompletableFuture[]::new)
					);
				});
			}

			@Override
			public String getName() {
				return "FetalStrike/Registry";
			}

			private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
					CachedOutput cachedOutput, HolderLookup.Provider provider, DynamicOps<JsonElement> dynamicOps, RegistryDataLoader.RegistryData<T> registryData
			) {
				ResourceKey<? extends Registry<T>> resourceKey = registryData.key();
				return provider.lookup(resourceKey)
						.map(
								registryLookup -> {
									PackOutput.PathProvider pathProvider = output.createRegistryElementsPathProvider(resourceKey);
									return CompletableFuture.allOf(
											registryLookup.listElements()
													.filter(reference -> reference.key().location().getNamespace().equals("fetalstrike"))
													.map(reference -> dumpValue(pathProvider.json(reference.key().location()), cachedOutput, dynamicOps, registryData.elementCodec(), (T) reference.value()))
													.toArray(CompletableFuture[]::new)
									);
								}
						);
			}

			private static <E> CompletableFuture<?> dumpValue(Path path, CachedOutput cachedOutput, DynamicOps<JsonElement> dynamicOps, Encoder<E> encoder, E object) {
				return encoder.encodeStart(dynamicOps, object)
						.mapOrElse(
								jsonElement -> DataProvider.saveStable(cachedOutput, jsonElement, path),
								error -> CompletableFuture.failedFuture(new IllegalStateException("Couldn't generate file '" + path + "': " + error.message()))
						);
			}
		});
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.ENCHANTMENT, FetalStrikeDataGenerator::bootstrapEnchant);
	}

	private static class AdvancementProvider extends FabricAdvancementProvider {

		protected AdvancementProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
			super(output, registryLookup);
		}

		@Override
		public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {
			Advancement.Builder.advancement().addCriterion(
							"kill_child",
							KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true)))
					).display(new ItemStack(Items.NETHERITE_SWORD),
							Component.translatable("fetalstrike.advancement.title"),
							Component.translatable("fetalstrike.advancement.description"),
							null,
							AdvancementType.TASK,
							true, true, true)
					.save(consumer, "fetalstrike/you_monster");
		}
	}


}