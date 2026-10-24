package com.minecraft.atlamod.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockEvent {
    private BlockEvent() {}

    public static final class BreakEvent extends CancellableEvent {
        private final Level level;
        private final BlockPos pos;
        private final BlockState state;
        private final Player player;

        public BreakEvent(Level level, BlockPos pos, BlockState state, Player player) {
            this.level = level;
            this.pos = pos;
            this.state = state;
            this.player = player;
        }

        public Level getLevel() { return level; }
        public BlockPos getPos() { return pos; }
        public BlockState getState() { return state; }
        public Player getPlayer() { return player; }
    }
}
