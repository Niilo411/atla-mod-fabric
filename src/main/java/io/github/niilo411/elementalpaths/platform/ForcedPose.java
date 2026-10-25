package io.github.niilo411.elementalpaths.platform;

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
    Pose elemental_paths$getForcedPose();

    void elemental_paths$setForcedPose(@Nullable Pose pose);

    static void set(Player player, @Nullable Pose pose) {
        ((ForcedPose) player).elemental_paths$setForcedPose(pose);
    }

    @Nullable
    static Pose get(Player player) {
        return ((ForcedPose) player).elemental_paths$getForcedPose();
    }
}
