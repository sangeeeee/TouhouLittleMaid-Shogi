package com.github.sangeeeee.tlm_shogi.loot;

import com.github.sangeeeee.tlm_shogi.datapack.TsumeBoardStateData;
import com.github.sangeeeee.tlm_shogi.datapack.TsumeBoardStateRecord;
import com.github.sangeeeee.tlm_shogi.init.InitDataComponents;
import com.github.sangeeeee.tlm_shogi.init.InitLootModifiers;
import com.github.tartaricacid.touhoulittlemaid.item.ItemBoardState;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/** Selects a weighted tsume position whose tags match at least one requested tag. */
public final class RandomTsumeBoardStateFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomTsumeBoardStateFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).and(Codec.STRING.listOf().fieldOf("tags").forGetter(value -> value.tags))
                    .apply(instance, RandomTsumeBoardStateFunction::new));

    private final List<String> tags;

    private RandomTsumeBoardStateFunction(List<LootItemCondition> conditions, List<String> tags) {
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
        List<TsumeBoardStateRecord> records = TsumeBoardStateData.records().stream().filter(this::matches).toList();
        TsumeBoardStateRecord selected = pick(records, context.getRandom());
        if (selected == null) {
            return stack;
        }
        TsumeBoardStateRecord.Display display = selected.display();
        ItemBoardState.setState(stack, selected.data(), display.description(), display.author());
        stack.set(InitDataComponents.TSUME_MAXIMUM_PLY, selected.maximumPly());
        return stack;
    }

    private static TsumeBoardStateRecord pick(List<TsumeBoardStateRecord> records, RandomSource random) {
        int totalWeight = records.stream().mapToInt(record -> Math.max(0, record.weight())).sum();
        if (records.isEmpty()) {
            return null;
        }
        if (totalWeight <= 0) {
            return records.get(random.nextInt(records.size()));
        }
        int value = random.nextInt(totalWeight);
        for (TsumeBoardStateRecord record : records) {
            value -= Math.max(0, record.weight());
            if (value < 0) {
                return record;
            }
        }
        return records.getLast();
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return InitLootModifiers.TSUME_BOARD_STATE_RANDOMLY.get();
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
            return new RandomTsumeBoardStateFunction(getConditions(), tags);
        }
    }
}
