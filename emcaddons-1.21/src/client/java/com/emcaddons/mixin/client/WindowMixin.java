package com.emcaddons.mixin.client;

import com.emcaddons.EmcAddonsClient;
import com.emcaddons.gui.WindowIcon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "setIcon(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/client/util/Icons;)V", at = @At("TAIL"))
    private void reapplyCraftologyWindowIcon(ResourcePack resourcePack, Icons icons, CallbackInfo ci) {
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod == null || !mod.isWindowIconEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        WindowIcon.apply(client, true);
    }
}
