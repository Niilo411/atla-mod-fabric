package com.minecraft.atlamod.mixin;

import com.minecraft.atlamod.WaterCanteenItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    /**
     * A canteen's capacity is a SETTING, so it is answered live rather than read off the
     * MAX_DAMAGE component stamped on the stack when it was made. Vanilla's clamping, the
     * durability bar and the tooltip all read through here. See WaterCanteenItem#capacity.
     */
    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void atlamod$liveCanteenCapacity(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof WaterCanteenItem canteen) {
            cir.setReturnValue(canteen.getMaxDamage(self));
        }
    }
}
