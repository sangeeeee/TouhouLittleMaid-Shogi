package com.github.sangeeeee.tlm_shogi.loot;

import com.github.sangeeeee.tlm_shogi.datapack.TsumeBoardStateData;
import com.github.sangeeeee.tlm_shogi.datapack.TsumeBoardStateRecord;
import com.github.sangeeeee.tlm_shogi.init.InitLootModifiers;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/** Selects only from the independent masterpiece catalog. */
public final class RandomTsumeMasterpieceFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomTsumeMasterpieceFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).and(Codec.STRING.listOf().fieldOf("tags").forGetter(value -> value.tags))
                    .apply(instance, RandomTsumeMasterpieceFunction::new));

    private final List<String> tags;

    private RandomTsumeMasterpieceFunction(List<LootItemCondition> conditions, List<String> tags) {
        super(conditions);
        this.tags = List.copyOf(tags);
    }

    private boolean matches(TsumeBoardStateRecord record) {
        return tags.stream().anyMatch(record.tags()::contains);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (tags.isEmpty()) {
            return stack;
        }
        List<TsumeBoardStateRecord> records = TsumeBoardStateData.masterpieces().stream()
                .filter(this::matches)
                .toList();
        TsumeBoardStateRecord selected = RandomTsumeBoardStateFunction.pick(records, context.getRandom());
        return selected == null ? stack : RandomTsumeBoardStateFunction.applyRecord(stack, selected, true);
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return InitLootModifiers.TSUME_MASTERPIECE_RANDOMLY.get();
    }

    public static final class Builder extends LootItemConditionalFunction.Builder<Builder> {
        private final List<String> tags = Lists.newArrayList();

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder addTag(String tag) {
            tags.add(tag);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new RandomTsumeMasterpieceFunction(getConditions(), tags);
        }
    }
}
