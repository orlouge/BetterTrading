package com.github.orlouge.bettertrading;

import net.minecraft.nbt.NbtCompound;

public interface ExtendedVillagerEntity {
    void readExtraData(NbtCompound nbt);
    void writeExtraData(NbtCompound nbt);
}
