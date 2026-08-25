package lkwarwick.deterministicenchanting.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import lkwarwick.deterministicenchanting.DeterministicEnchanting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(EnchantmentMenu.class)
public class EnchantmentScreenHandlerMixin {
    @Shadow
    @Final
    private ContainerLevelAccess access;

    private enum EnchantmentState {
        COMPATIBLE,
        INSUFFICIENT_BOOKSHELVES,
        ALREADY_PRESENT,
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

        for (int power = 1; power <= 30; power++) {
            var available = EnchantmentHelper.getAvailableEnchantmentResults(
                power,
                itemStack,
                registry.listElements().map(
                    holder -> (Holder<Enchantment>) holder
                )
            );

            for (var enchantment : available) {
                var name = enchantment.enchantment().value()
                    .description()
                    .getString();

                var key = name + " " + enchantment.level();
                requiredPowers.putIfAbsent(key, power);
                states.putIfAbsent(key, stateFor(enchantment, itemStack, power, bookshelfPower[0]));
            }
        }

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
        int bookshelfPower
    ) {
        if (EnchantmentHelper.getItemEnchantmentLevel(enchantment.enchantment(), itemStack) > 0) {
            return EnchantmentState.ALREADY_PRESENT;
        }

        if (requiredPower > bookshelfPower) {
            return EnchantmentState.INSUFFICIENT_BOOKSHELVES;
        }

        return EnchantmentState.COMPATIBLE;
    }
}