package lkwarwick.deterministicenchanting.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(EnchantmentMenu.class)
public class EnchantmentScreenHandlerMixin {
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
        Map<String, Integer> enchantments = new LinkedHashMap<>();

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
                enchantments.putIfAbsent(key, power);
            }
        }

        System.out.println("=== AVAILABLE ENCHANTMENTS ===");

        enchantments.forEach((name, power) ->
            System.out.println(name + " -> power " + power)
        );

        System.out.println("==============================");
    }
}