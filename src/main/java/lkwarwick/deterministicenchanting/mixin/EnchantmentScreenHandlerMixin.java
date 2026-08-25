package lkwarwick.deterministicenchanting.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import lkwarwick.deterministicenchanting.DeterministicEnchanting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(EnchantmentMenu.class)
public class EnchantmentScreenHandlerMixin {
    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Unique
    private Player deterministicEnchanting$player;

    private enum EnchantmentState {
        COMPATIBLE,
        INSUFFICIENT_BOOKSHELVES,
        ALREADY_PRESENT,
        UPGRADE_AVAILABLE,
        INSUFFICIENT_LEVELS,
        INCOMPATIBLE
    }

    @Inject(
        method = "getEnchantmentList",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inspectEnchantments(
        RegistryAccess access,
        ItemStack itemStack,
        int slot,
        int enchantmentCost,
        CallbackInfoReturnable<List<EnchantmentInstance>> cir
    ) {
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Integer> requiredPowers = new LinkedHashMap<>();
        Map<String, EnchantmentState> states = new LinkedHashMap<>();
        int[] bookshelfPower = {0};

        this.access.execute((level, tablePosition) -> {
            for (var bookshelfOffset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                if (EnchantingTableBlock.isValidBookShelf(level, tablePosition, bookshelfOffset)) {
                    bookshelfPower[0]++;
                }
            }
        });

        registry.listElements().forEach(holder -> {
            var definition = holder.value();
            if (!definition.canEnchant(itemStack)) {
                return;
            }

            for (int level = definition.getMinLevel(); level <= definition.getMaxLevel(); level++) {
                var enchantment = new EnchantmentInstance(holder, level);
                var name = definition.description().getString();
                var key = name + " " + level;
                var requiredPower = definition.getMinCost(level);

                requiredPowers.putIfAbsent(key, requiredPower);
                states.putIfAbsent(
                    key,
                    stateFor(enchantment, itemStack, requiredPower, bookshelfPower[0], deterministicEnchanting$player, enchantmentCost)
                );
            }
        });

        DeterministicEnchanting.LOGGER.info(
            "Discovered {} deterministic enchantment options for slot {} (bookshelf power: {}, vanilla cost: {}, item: {})",
            requiredPowers.size(),
            slot,
            bookshelfPower[0],
            enchantmentCost,
            itemStack.getItem()
        );

        requiredPowers.forEach((name, requiredPower) ->
            DeterministicEnchanting.LOGGER.info(
                "  {} -> state {}, minimum power {}",
                name,
                states.get(name),
                requiredPower
            )
        );
    }

    private static EnchantmentState stateFor(
        EnchantmentInstance enchantment,
        ItemStack itemStack,
        int requiredPower,
        int bookshelfPower,
        Player player,
        int enchantmentCost
    ) {
        var currentLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment.enchantment(), itemStack);
        if (currentLevel >= enchantment.level()) {
            return EnchantmentState.ALREADY_PRESENT;
        }

        if (requiredPower > bookshelfPower) {
            return EnchantmentState.INSUFFICIENT_BOOKSHELVES;
        }

        if (currentLevel > 0) {
            return EnchantmentState.UPGRADE_AVAILABLE;
        }

        if (player != null && !player.hasInfiniteMaterials() && player.experienceLevel < enchantmentCost) {
            return EnchantmentState.INSUFFICIENT_LEVELS;
        }

        return EnchantmentState.COMPATIBLE;
    }

    @Inject(method = "stillValid", at = @At("HEAD"))
    private void rememberPlayer(Player player, CallbackInfoReturnable<Boolean> cir) {
        deterministicEnchanting$player = player;
    }

    @Redirect(
        method = "slotsChanged",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEnchantable()Z")
    )
    private boolean allowAlreadyEnchantedItems(ItemStack itemStack) {
        return itemStack.isEnchantable() || itemStack.isEnchanted();
    }
}