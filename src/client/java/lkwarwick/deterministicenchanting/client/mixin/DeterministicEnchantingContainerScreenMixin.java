package lkwarwick.deterministicenchanting.client.mixin;

import lkwarwick.deterministicenchanting.client.DeterministicEnchantingScrollable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class DeterministicEnchantingContainerScreenMixin {
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void scrollDeterministicOptions(
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (((Object) this) instanceof DeterministicEnchantingScrollable scrollable
            && scrollable.deterministicEnchanting$scroll(mouseX, mouseY, scrollY)) {
            cir.setReturnValue(true);
        }
    }
}
