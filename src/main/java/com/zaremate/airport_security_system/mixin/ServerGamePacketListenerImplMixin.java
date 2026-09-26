package com.zaremate.airport_security_system.mixin;

import com.zaremate.airport_security_system.AirportSecuritySystemEvents;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Inject(method = "handleSignUpdate", at = @At("HEAD"), cancellable = true)
    private void airport_security_system$handleSignUpdate(ServerboundSignUpdatePacket packet, CallbackInfo ci) {
        ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl) (Object) this;
        if (!AirportSecuritySystemEvents.isExpectedPacket(listener.player, packet))
            return;

        listener.player.server.execute(() ->
                AirportSecuritySystemEvents.handleSignResponse(listener.player, packet));
        ci.cancel();
    }
}
