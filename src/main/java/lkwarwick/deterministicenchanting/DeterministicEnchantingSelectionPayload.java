package lkwarwick.deterministicenchanting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DeterministicEnchantingSelectionPayload(int menuId, Identifier enchantment, int level)
    implements CustomPacketPayload {
    public static final Type<DeterministicEnchantingSelectionPayload> TYPE =
        new Type<>(DeterministicEnchanting.id("select_enchantment"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeterministicEnchantingSelectionPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DeterministicEnchantingSelectionPayload::menuId,
            Identifier.STREAM_CODEC,
            DeterministicEnchantingSelectionPayload::enchantment,
            ByteBufCodecs.VAR_INT,
            DeterministicEnchantingSelectionPayload::level,
            DeterministicEnchantingSelectionPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
