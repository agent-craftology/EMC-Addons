package com.emcaddons.mixin;

import com.emcaddons.EmcAddonsClient;
import com.emcaddons.gui.WindowIcon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.resource.InputSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "setIcon(Lnet/minecraft/resource/InputSupplier;Lnet/minecraft/resource/InputSupplier;)V", at = @At("TAIL"))
    private void applyCraftologyWindowIcon(InputSupplier<InputStream> icon16, InputSupplier<InputStream> icon32, CallbackInfo ci) {
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod == null || !mod.isWindowIconEnabled()) return;
        WindowIcon.apply(MinecraftClient.getInstance(), true);
    }
}
