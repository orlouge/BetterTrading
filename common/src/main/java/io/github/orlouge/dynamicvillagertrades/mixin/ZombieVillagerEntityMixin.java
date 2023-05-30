package io.github.orlouge.dynamicvillagertrades.mixin;

import io.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import io.github.orlouge.dynamicvillagertrades.ExtendedVillagerEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ZombieVillagerEntity.class)
public class ZombieVillagerEntityMixin implements ExtendedVillagerEntity {
    @Shadow private NbtCompound offerData;
    private static final String extra_data_key = DynamicVillagerTradesMod.MOD_ID + "_data";
    private NbtCompound extraData = null;

    @Inject(method = "finishConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/VillagerEntity;setExperience(I)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void transferOffers(ServerWorld world, CallbackInfo ci, VillagerEntity villager) {
        if (extraData != null && villager instanceof ExtendedVillagerEntity extendedVillager) {
            extendedVillager.readExtraData(this.extraData);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void onWriteCustomData(NbtCompound nbt, CallbackInfo info) {
        writeExtraData(nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void onReadCustomData(NbtCompound nbt, CallbackInfo info) {
        readExtraData(nbt);
    }

    @Override
    public void readExtraData(NbtCompound nbt) {
        if (this.extraData == null) {
            this.extraData = new NbtCompound();
        }
        this.extraData.copyFrom(nbt);
    }

    @Override
    public void writeExtraData(NbtCompound nbt) {
        if (this.extraData != null) {
            nbt.copyFrom(this.extraData);
        }
    }
}
