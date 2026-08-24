package com.emcaddons.mixin;

import com.emcaddons.EmcAddonsClient;
import com.emcaddons.gui.WindowIcon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "setIcon(Ljava/io/InputStream;Ljava/io/InputStream;)V", at = @At("TAIL"))
    private void applyCraftologyWindowIcon(InputStream icon16, InputStream icon32, CallbackInfo ci) {
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod == null || !mod.isWindowIconEnabled()) return;
        WindowIcon.apply(MinecraftClient.getInstance(), true);
    }
}
