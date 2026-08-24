package com.emcaddons.mixin;

import com.emcaddons.EmcAddonsClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts outgoing chat packets so {@code /config} never reaches the server.
 * 1.18.2 has no {@code ClientPlayNetworkHandler.sendChatMessage}; chat is sent as {@link ChatMessageC2SPacket}.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void emcaddons$swallowConfigChat(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ChatMessageC2SPacket chat)) return;
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod != null && mod.handleOutgoingChatMessage(chat.getChatMessage())) {
            ci.cancel();
        }
    }
}
