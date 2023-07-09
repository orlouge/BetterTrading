package io.github.orlouge.dynamicvillagertrades.fabric.mixin;

import io.github.orlouge.dynamicvillagertrades.ExtendedVillagerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ZombieEntity.class)
public class ZombieEntityMixin {
    @Inject(method = "onKilledOther", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/ZombieVillagerEntity;setXp(I)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void transferExtraData(ServerWorld world, LivingEntity other, CallbackInfoReturnable<Boolean> cir, boolean bl, VillagerEntity villagerEntity, ZombieVillagerEntity zombieVillagerEntity)
    {
        if (other instanceof ExtendedVillagerEntity extendedVillager && zombieVillagerEntity instanceof ExtendedVillagerEntity extendedZombieVillager) {
            NbtCompound extraData = new NbtCompound();
            extendedVillager.writeExtraData(extraData);
            extendedZombieVillager.readExtraData(extraData);
        }
    }
}
