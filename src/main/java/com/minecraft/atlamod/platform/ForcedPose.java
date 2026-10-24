package com.minecraft.atlamod.platform;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge's Player#setForcedPose, which Fabric does not have.
 *
 * Player#updatePlayerPose recomputes a player's pose from scratch every tick on both
 * sides, so a plain setPose is undone immediately. A forced pose is checked FIRST in
 * that method (PlayerMixin) and wins outright; null hands the pose back to vanilla.
 */
public interface ForcedPose {
    @Nullable
    Pose atlamod$getForcedPose();

    void atlamod$setForcedPose(@Nullable Pose pose);

    static void set(Player player, @Nullable Pose pose) {
        ((ForcedPose) player).atlamod$setForcedPose(pose);
    }

    @Nullable
    static Pose get(Player player) {
        return ((ForcedPose) player).atlamod$getForcedPose();
    }
}
