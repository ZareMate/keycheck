package com.zaremate.keycheck.mixin;

import com.zaremate.keycheck.KeyCheckEvents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void keycheck$holdLoadingScreen(Packet<?> packet, CallbackInfo ci) {
        if (!((Object) this instanceof ServerGamePacketListenerImpl connection))
            return;
        if (!(packet instanceof ClientboundGameEventPacket gameEvent))
            return;
        if (KeyCheckEvents.isReleasingLoadingScreen(connection))
            return;
        if (KeyCheckEvents.holdLoadingScreen(connection, gameEvent))
            ci.cancel();
    }
}
