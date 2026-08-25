package lkwarwick.deterministicenchanting;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

public interface DeterministicEnchantingMenu {
    boolean applyDeterministicEnchantment(Player player, Holder<Enchantment> enchantment, int level);
}
