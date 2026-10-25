package io.github.niilo411.elementalpaths.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class PlayerInteractEvent {
    private PlayerInteractEvent() {}

    public static final class RightClickBlock extends CancellableEvent {
        private final Player player;
        private final InteractionHand hand;
        private final BlockPos pos;
        private InteractionResult result = InteractionResult.PASS;

        public RightClickBlock(Player player, InteractionHand hand, BlockPos pos) {
            this.player = player;
            this.hand = hand;
            this.pos = pos;
        }

        public Player getEntity() { return player; }
        public Level getLevel() { return player.level(); }
        public InteractionHand getHand() { return hand; }
        public BlockPos getPos() { return pos; }
        public void setCancellationResult(InteractionResult result) { this.result = result; }
        public InteractionResult getCancellationResult() { return result; }
    }
}
