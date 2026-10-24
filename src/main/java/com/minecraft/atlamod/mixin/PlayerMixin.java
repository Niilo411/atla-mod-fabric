package com.minecraft.atlamod.mixin;

import com.minecraft.atlamod.platform.ForcedPose;
import com.minecraft.atlamod.platform.ServerHooks;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin implements ForcedPose {
    @Unique
    @Nullable
    private Pose atlamod$forcedPose;

    /** NeoForge's PlayerTickEvent.Post: the end of every player tick, on BOTH sides. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void atlamod$tickPost(CallbackInfo ci) {
        ServerHooks.playerTick((Player) (Object) this);
    }

    /** A forced pose wins before vanilla looks at anything else. See ForcedPose. */
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void atlamod$forcedPose(CallbackInfo ci) {
        if (atlamod$forcedPose != null) {
            ((Player) (Object) this).setPose(atlamod$forcedPose);
            ci.cancel();
        }
    }

    @Override
    public @Nullable Pose atlamod$getForcedPose() {
        return atlamod$forcedPose;
    }

    @Override
    public void atlamod$setForcedPose(@Nullable Pose pose) {
        atlamod$forcedPose = pose;
    }
}
