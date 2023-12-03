package io.github.orlouge.dynamicvillagertrades.trade_offers;

import io.github.orlouge.dynamicvillagertrades.DynamicVillagerTradesMod;
import io.github.orlouge.dynamicvillagertrades.api.SerializableTradeOfferFactory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class EnchantSpecificBookFactory implements SerializableTradeOfferFactory {
    private final int experience;
    private final Identifier type;
    private final int level;

    public static final Codec<EnchantSpecificBookFactory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("experience", 2).forGetter(EnchantSpecificBookFactory::getExperience),
            Identifier.CODEC.fieldOf("enchantment").forGetter(EnchantSpecificBookFactory::getEnchantmentType),
            Codec.INT.optionalFieldOf("level", 1).forGetter(EnchantSpecificBookFactory::getLevel)
    ).apply(instance, EnchantSpecificBookFactory::new));

    public EnchantSpecificBookFactory(int experience, Identifier type, int level) {
        this.experience = experience;
        this.type = type;
        this.level = level;
    }

    @Override
    public Supplier<TradeOfferFactoryType<?>> getType() {
        return TradeOfferFactoryType.ENCHANT_SPECIFIC_BOOK::get;
    }

    @Nullable
    @Override
    public TradeOffer create(Entity entity, Random random) {
        Enchantment enchantment = Registry.ENCHANTMENT.get(this.type);
        if (enchantment == null) {
            throw new IllegalStateException("Enchantment " + this.type + " does not exist.");
        }
        ItemStack itemStack = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, this.level));
        if (DynamicVillagerTradesMod.ENCHANT_REPAIR_COMPAT) {
            EnchantedBookItem.addEnchantment(itemStack, new EnchantmentLevelEntry(Enchantments.VANISHING_CURSE, 1));
        }
        int j = 2 + random.nextInt(5 + this.level * 10) + 3 * this.level;
        if (enchantment.isTreasure()) {
            j *= 2;
        }

        if (j > 64) {
            j = 64;
        }

        return new TradeOffer(new ItemStack(Items.EMERALD, j), new ItemStack(Items.BOOK), itemStack, 12, this.experience, 0.2F);
    }

    public int getExperience() {
        return experience;
    }

    public Identifier getEnchantmentType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return this.type.toString() + ":" + this.level;
    }
}
