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
import java.util.Objects;

@Mixin(EnchantmentScreen.class)
public class DeterministicEnchantingScreenMixin {
    @Unique
    private static final int PANEL_WIDTH = 164;

    @Unique
    private static final int ROW_HEIGHT = 16;

    @Unique
    private static final int GROUP_GAP = 5;

    @Unique
    private static final int GROUP_HEADER_HEIGHT = 10;

    @Unique
    private static final int CONTENT_INSET = 10;

    @Unique
    private static final int HEADER_LEVEL_GAP = 4;

    @Unique
    private static final int MAX_VISIBLE_ROWS = 22;

    @Unique
    private record DeterministicEnchanting$Option(Identifier id, int level, String name, String detail, int cost, String state, int color) {
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

        var visibleOptions = Math.min(options.size(), MAX_VISIBLE_ROWS);
        var panelHeight = deterministicEnchanting$contentHeight(options, visibleOptions) + 12;
        var panelX = deterministicEnchanting$panelX(graphics.guiWidth());
        var panelY = Math.max(4, (graphics.guiHeight() - panelHeight) / 2);

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xDD101820);
        graphics.outline(panelX, panelY, PANEL_WIDTH, panelHeight, 0xFF8FA6B8);

        for (int index = 0; index < visibleOptions; index++) {
            var option = options.get(index);
            var rowY = panelY + 6 + deterministicEnchanting$rowOffset(options, index);
            if (index == 0 || !Objects.equals(option.id(), options.get(index - 1).id())) {
                var headerY = rowY - GROUP_HEADER_HEIGHT;
                var headerColor = 0xFFE5C07B;
                var name = Objects.requireNonNull(option.name());
                graphics.text(minecraft.font, name, panelX + CONTENT_INSET, headerY, headerColor);
                graphics.horizontalLine(
                    panelX + CONTENT_INSET,
                    panelX + CONTENT_INSET + minecraft.font.width(name),
                    headerY + 9,
                    headerColor
                );
            }
            var hovered = mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(panelX + 2, rowY, panelX + PANEL_WIDTH - 2, rowY + ROW_HEIGHT, 0xFF4C566A);
            }
            var owned = option.state().equals("OWNED");
            var stateColor = owned ? 0xFF8A8A8A : option.color();
            graphics.fill(panelX + CONTENT_INSET - 3, rowY + 2, panelX + CONTENT_INSET - 1, rowY + ROW_HEIGHT - 2, stateColor);
            var detail = Objects.requireNonNull(option.detail());
            graphics.text(
                minecraft.font,
                detail,
                panelX + CONTENT_INSET,
                rowY + (ROW_HEIGHT - minecraft.font.lineHeight) / 2,
                stateColor
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
        var visibleOptions = Math.min(options.size(), MAX_VISIBLE_ROWS);
        var panelHeight = deterministicEnchanting$contentHeight(options, visibleOptions) + 12;
        var panelX = deterministicEnchanting$panelX(minecraft.getWindow().getGuiScaledWidth());
        var panelY = Math.max(4, (minecraft.getWindow().getGuiScaledHeight() - panelHeight) / 2);
        var row = deterministicEnchanting$rowAt(options, visibleOptions, event.y() - panelY - 6);

        if (event.x() < panelX || event.x() >= panelX + PANEL_WIDTH
            || row < 0) {
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

        var clientLevel = Objects.requireNonNull(minecraft.level);
        var registry = clientLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var player = Objects.requireNonNull(minecraft.player);
        return registry.listElements()
            .filter(holder -> holder.value().canEnchant(itemStack))
            .flatMap(holder -> java.util.stream.IntStream.rangeClosed(
                holder.value().getMinLevel(),
                holder.value().getMaxLevel()
            ).mapToObj(level -> {
                var currentLevel = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(holder, itemStack);
                var cost = holder.value().getMinCost(level);
                var affordable = player.experienceLevel >= cost;
                var state = currentLevel >= level
                    ? "OWNED"
                    : currentLevel > 0 && !affordable
                        ? "UPGRADE / XP LOW"
                        : currentLevel > 0
                            ? "UPGRADE"
                            : !affordable
                                ? "XP LOW"
                                : "NEW";
                var detail = currentLevel >= level
                    ? " Level " + level + " - Already applied"
                    : " Level " + level + " - Requires " + cost + " levels";
                var color = state.equals("OWNED")
                    ? 0xFF8FBC8F
                    : state.contains("XP LOW")
                        ? 0xFFE06C75
                        : state.equals("UPGRADE")
                            ? 0xFFE5C07B
                            : 0xFF88C0D0;
                return new DeterministicEnchanting$Option(
                    holder.unwrapKey().orElseThrow().identifier(),
                    level,
                    holder.value().description().getString(),
                    detail,
                    cost,
                    state,
                    color
                );
            }))
            .toList();
    }

    @Unique
    private int deterministicEnchanting$panelX(int guiWidth) {
        return Math.max(4, (guiWidth - PANEL_WIDTH) / 2 - 180);
    }

    @Unique
    private int deterministicEnchanting$contentHeight(List<DeterministicEnchanting$Option> options, int visibleOptions) {
        return visibleOptions * ROW_HEIGHT
            + deterministicEnchanting$groupCount(options, visibleOptions) * (GROUP_HEADER_HEIGHT + HEADER_LEVEL_GAP)
            + Math.max(0, deterministicEnchanting$groupCount(options, visibleOptions) - 1) * GROUP_GAP;
    }

    @Unique
    private int deterministicEnchanting$rowOffset(List<DeterministicEnchanting$Option> options, int index) {
        var offset = GROUP_HEADER_HEIGHT + HEADER_LEVEL_GAP;
        for (int previous = 1; previous <= index; previous++) {
            if (!Objects.equals(options.get(previous).id(), options.get(previous - 1).id())) {
            offset += GROUP_HEADER_HEIGHT + HEADER_LEVEL_GAP + GROUP_GAP;
            }
        }
        return index * ROW_HEIGHT + offset;
    }

    @Unique
    private int deterministicEnchanting$groupCount(List<DeterministicEnchanting$Option> options, int visibleOptions) {
        var groups = 0;
        for (int index = 0; index < visibleOptions; index++) {
            if (index == 0 || !Objects.equals(options.get(index).id(), options.get(index - 1).id())) {
                groups++;
            }
        }
        return groups;
    }

    @Unique
    private int deterministicEnchanting$rowAt(List<DeterministicEnchanting$Option> options, int visibleOptions, double y) {
        for (int index = 0; index < visibleOptions; index++) {
            var rowY = deterministicEnchanting$rowOffset(options, index);
            if (y >= rowY && y < rowY + ROW_HEIGHT) {
                return index;
            }
        }
        return -1;
    }
}
