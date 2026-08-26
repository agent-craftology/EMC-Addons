package com.emcaddons.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emcaddons.EmcAddonsClient;

/**
 * Intercepts outgoing chat so {@code /config} and {@code /emczone} never reach the server.
 */
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void emcaddons$swallowConfigChat(String content, CallbackInfo ci) {
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod != null && mod.handleOutgoingChatMessage(content)) {
            ci.cancel();
        }
    }
}
