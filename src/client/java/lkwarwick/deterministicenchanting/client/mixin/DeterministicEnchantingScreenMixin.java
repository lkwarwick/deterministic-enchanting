package lkwarwick.deterministicenchanting.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import lkwarwick.deterministicenchanting.DeterministicEnchantingSelectionPayload;

import java.util.List;

@Mixin(EnchantmentScreen.class)
public class DeterministicEnchantingScreenMixin {
    @Unique
    private static final int PANEL_WIDTH = 132;

    @Unique
    private static final int ROW_HEIGHT = 10;

    @Unique
    private static final int MAX_VISIBLE_ROWS = 22;

    @Unique
    private record DeterministicEnchanting$Option(Identifier id, int level, String label) {
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderDeterministicOptions(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick,
        CallbackInfo info
    ) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var menu = ((EnchantmentScreen) (Object) this).getMenu();
        var itemStack = menu.getSlot(0).getItem();
        if (itemStack.isEmpty()) {
            return;
        }

        var options = deterministicEnchanting$options(itemStack);

        var panelHeight = Math.min(options.size(), MAX_VISIBLE_ROWS) * ROW_HEIGHT + 24;
        var panelX = deterministicEnchanting$panelX(graphics.guiWidth());
        var panelY = Math.max(4, (graphics.guiHeight() - panelHeight) / 2);

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xDD101820);
        graphics.outline(panelX, panelY, PANEL_WIDTH, panelHeight, 0xFF8FA6B8);
        graphics.text(minecraft.font, "Deterministic options", panelX + 6, panelY + 6, 0xFFE5C07B);

        for (int index = 0; index < Math.min(options.size(), MAX_VISIBLE_ROWS); index++) {
            graphics.text(
                minecraft.font,
                options.get(index).label(),
                panelX + 6,
                panelY + 18 + index * ROW_HEIGHT,
                0xFFD8DEE9
            );
        }

        if (options.size() > MAX_VISIBLE_ROWS) {
            graphics.text(
                minecraft.font,
                "+" + (options.size() - MAX_VISIBLE_ROWS) + " more",
                panelX + 6,
                panelY + panelHeight - 12,
                0xFF8FA6B8
            );
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void selectDeterministicOption(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var options = deterministicEnchanting$options(((EnchantmentScreen) (Object) this).getMenu().getSlot(0).getItem());
        var panelHeight = Math.min(options.size(), MAX_VISIBLE_ROWS) * ROW_HEIGHT + 24;
        var panelX = deterministicEnchanting$panelX(minecraft.getWindow().getGuiScaledWidth());
        var panelY = Math.max(4, (minecraft.getWindow().getGuiScaledHeight() - panelHeight) / 2);
        var row = (int) ((event.y() - panelY - 18) / ROW_HEIGHT);

        if (event.x() < panelX || event.x() >= panelX + PANEL_WIDTH
            || event.y() < panelY + 18 || row < 0 || row >= Math.min(options.size(), MAX_VISIBLE_ROWS)) {
            return;
        }

        var option = options.get(row);
        ClientPlayNetworking.send(new DeterministicEnchantingSelectionPayload(
            ((EnchantmentScreen) (Object) this).getMenu().containerId,
            option.id(),
            option.level()
        ));
        cir.setReturnValue(true);
    }

    @Unique
    private List<DeterministicEnchanting$Option> deterministicEnchanting$options(net.minecraft.world.item.ItemStack itemStack) {
        var minecraft = Minecraft.getInstance();
        if (itemStack.isEmpty() || minecraft.level == null) {
            return List.of();
        }

        var clientLevel = minecraft.level;
        var registry = clientLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return registry.listElements()
            .filter(holder -> holder.value().canEnchant(itemStack))
            .flatMap(holder -> java.util.stream.IntStream.rangeClosed(
                holder.value().getMinLevel(),
                holder.value().getMaxLevel()
            ).mapToObj(level -> new DeterministicEnchanting$Option(
                holder.unwrapKey().orElseThrow().identifier(),
                level,
                holder.value().description().getString() + " " + level
            )))
            .toList();
    }

    @Unique
    private int deterministicEnchanting$panelX(int guiWidth) {
        return Math.max(4, (guiWidth - PANEL_WIDTH) / 2 - 156);
    }
}
