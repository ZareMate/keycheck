package com.zaremate.airport_security_system.mixin;

import com.zaremate.airport_security_system.client.KeyCheckClient;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NeoForgeLoadingOverlay.class)
public abstract class NeoForgeLoadingOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void airport_security_system$renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        KeyCheckClient.render(graphics);
    }
}
