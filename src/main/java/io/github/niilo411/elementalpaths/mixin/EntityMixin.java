package io.github.niilo411.elementalpaths.mixin;

import io.github.niilo411.elementalpaths.platform.ServerHooks;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    private Entity vehicle;

    /**
     * NeoForge's EntityMountEvent for a DISMOUNT. Every way off a vehicle ends here —
     * crouching, stopRiding, the vehicle being discarded — so this is the one place a
     * refusal holds for all of them. Earth trap and Gravity orbit are the two users.
     */
    @Inject(method = "removeVehicle", at = @At("HEAD"), cancellable = true)
    private void elemental_paths$dismount(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (vehicle != null && !self.level().isClientSide()
                && ServerHooks.dismountCancelled(self, vehicle)) {
            ci.cancel();
        }
    }
}
