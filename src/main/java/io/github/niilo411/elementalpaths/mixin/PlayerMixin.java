package io.github.niilo411.elementalpaths.mixin;

import io.github.niilo411.elementalpaths.platform.ForcedPose;
import io.github.niilo411.elementalpaths.platform.ServerHooks;
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
    private Pose elemental_paths$forcedPose;

    /** NeoForge's PlayerTickEvent.Post: the end of every player tick, on BOTH sides. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void elemental_paths$tickPost(CallbackInfo ci) {
        ServerHooks.playerTick((Player) (Object) this);
    }

    /** A forced pose wins before vanilla looks at anything else. See ForcedPose. */
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void elemental_paths$forcedPose(CallbackInfo ci) {
        if (elemental_paths$forcedPose != null) {
            ((Player) (Object) this).setPose(elemental_paths$forcedPose);
            ci.cancel();
        }
    }

    @Override
    public @Nullable Pose elemental_paths$getForcedPose() {
        return elemental_paths$forcedPose;
    }

    @Override
    public void elemental_paths$setForcedPose(@Nullable Pose pose) {
        elemental_paths$forcedPose = pose;
    }
}
