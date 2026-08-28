package com.emcaddons.mixin;

import com.emcaddons.EmcAddonsClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts outgoing chat packets so {@code /config} never reaches the server.
 * 1.18.2 has no {@code ClientPlayNetworkHandler.sendChatMessage}; chat is sent as {@link ChatMessageC2SPacket}.
 * Incoming {@link GameMessageS2CPacket} is cancelled only while a hidden {@code /zone reset} query is pending.
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

    @Inject(
            method = "onGameMessage(Lnet/minecraft/network/packet/s2c/play/GameMessageS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void emcaddons$hideZoneResetChat(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet == null) return;
        Text message = packet.getMessage();
        if (message == null) return;
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod != null && mod.getDungeonZoneScoreboard().onGameMessage(message.getString())) {
            ci.cancel();
        }
    }
}
