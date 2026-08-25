package lkwarwick.deterministicenchanting;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeterministicEnchanting implements ModInitializer {
	public static final String MOD_ID = "deterministic-enchanting";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		PayloadTypeRegistry.serverboundPlay().register(
			DeterministicEnchantingSelectionPayload.TYPE,
			DeterministicEnchantingSelectionPayload.CODEC
		);
		ServerPlayNetworking.registerGlobalReceiver(
			DeterministicEnchantingSelectionPayload.TYPE,
			(payload, context) -> context.server().execute(() -> {
				var player = context.player();
				if (player.containerMenu.containerId != payload.menuId()
					|| !(player.containerMenu instanceof DeterministicEnchantingMenu menu)) {
					return;
				}

				player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
					.get(payload.enchantment())
					.ifPresent(holder -> menu.applyDeterministicEnchantment(player, holder, payload.level()));
			})
		);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
