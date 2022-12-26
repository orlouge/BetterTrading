package com.github.orlouge.dynamicvillagertrades;

import net.minecraft.nbt.NbtCompound;

public interface ExtendedVillagerEntity {
    void readExtraData(NbtCompound nbt);
    void writeExtraData(NbtCompound nbt);
}
