package com.minecraft.atlamod.mixin;

import com.minecraft.atlamod.platform.ServerHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    /**
     * NeoForge's EntityTravelToDimensionEvent: BEFORE the move, while the player is still
     * standing in the old level with their state intact. Only a change of DIMENSION
     * counts; an ordinary teleport within the level is not a threshold.
     */
    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("HEAD"), cancellable = true)
    private void atlamod$beforeDimensionChange(TeleportTransition transition,
                                               CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (transition.newLevel() != self.level()
                && ServerHooks.travelCancelled(self, transition.newLevel().dimension())) {
            cir.setReturnValue(null);
        }
    }
}
