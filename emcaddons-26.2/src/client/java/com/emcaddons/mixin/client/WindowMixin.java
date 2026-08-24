package com.emcaddons.mixin.client;

import com.emcaddons.EmcAddonsClient;
import com.emcaddons.gui.WindowIcon;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "setIcon(Lnet/minecraft/server/packs/PackResources;Lcom/mojang/blaze3d/platform/IconSet;)V", at = @At("TAIL"))
    private void reapplyCraftologyWindowIcon(PackResources resourcePack, IconSet icons, CallbackInfo ci) {
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod == null || !mod.isWindowIconEnabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        WindowIcon.apply(client, true);
    }
}
