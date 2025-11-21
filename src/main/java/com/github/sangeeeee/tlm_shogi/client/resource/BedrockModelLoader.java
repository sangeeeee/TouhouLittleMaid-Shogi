package com.github.sangeeeee.tlm_shogi.client.resource;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.tartaricacid.simplebedrockmodel.client.manager.BedrockEntityModelRegister;
import com.github.tartaricacid.simplebedrockmodel.client.manager.BedrockEntityModelRegisterEvent;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.model.BroomModel;
import com.github.tartaricacid.touhoulittlemaid.client.model.EntityBoxModel;
import com.github.tartaricacid.touhoulittlemaid.client.model.EntityFairyModel;
import com.github.tartaricacid.touhoulittlemaid.client.model.NewEntityFairyModel;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

/**
 * 把所有硬编码的模型全部资源包化，方便资源包替换模型
 */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class BedrockModelLoader {
    // 内部数据
    private static final Map<ResourceLocation, Function<InputStream, ? extends SimpleBedrockModel<? extends Entity>>> ALL_MODELS = Maps.newHashMap();

    // 注册数据
    public static final ResourceLocation JCHESS = registerSimpleBlockModel("jchess");
    public static final ResourceLocation JCHESS_PIECES = registerSimpleBlockModel("jchess_pieces");

    public static ResourceLocation registerSimpleBlockModel(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(TouhouLittleMaidShogi.MOD_ID, "bedrock/block/" + name);
        return registerSimpleModel(location);
    }

    public static ResourceLocation registerSimpleEntityModel(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(TouhouLittleMaidShogi.MOD_ID, "bedrock/entity/" + name);
        return registerSimpleModel(location);
    }

    public static ResourceLocation registerSimpleModel(ResourceLocation location) {
        return registerModel(location, SimpleBedrockModel::new);
    }

    public static ResourceLocation registerBlockModel(String name, Function<InputStream, ? extends SimpleBedrockModel<? extends Entity>> function) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(TouhouLittleMaidShogi.MOD_ID, "bedrock/block/" + name);
        return registerModel(location, function);
    }

    public static ResourceLocation registerEntityModel(String name, Function<InputStream, ? extends SimpleBedrockModel<? extends Entity>> function) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(TouhouLittleMaidShogi.MOD_ID, "bedrock/entity/" + name);
        return registerModel(location, function);
    }

    public static ResourceLocation registerModel(ResourceLocation location, Function<InputStream, ? extends SimpleBedrockModel<? extends Entity>> function) {
        ALL_MODELS.put(location, function);
        return location;
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onRegisterBedrockModelRenderers(BedrockEntityModelRegisterEvent event) {
        ALL_MODELS.forEach(event::register);
        ALL_MODELS.clear();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Entity> SimpleBedrockModel<T> getModel(ResourceLocation location) {
        return (SimpleBedrockModel<T>) BedrockEntityModelRegister.INSTANCE.getModel(location);
    }
}
