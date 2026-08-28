package com.github.sangeeeee.tlm_shogi.client.model;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.sangeeeee.tlm_shogi.client.resource.BedrockModelLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

public class JChessPiecesModel {
    private final BedrockPart main;

    public JChessPiecesModel(String name) {
        SimpleBedrockModel<Entity> model = BedrockModelLoader.getModel(BedrockModelLoader.JCHESS_PIECES);
        this.main = Objects.requireNonNull(model).getPart(name);
    }

    public static JChessPiecesModel[] initModel() {
        JChessPiecesModel[] models = new JChessPiecesModel[38];

        models[10] = new JChessPiecesModel("KING_B");
        models[11] = new JChessPiecesModel("GOLD_B");
        models[12] = new JChessPiecesModel("SILVER_B");
        models[13] = new JChessPiecesModel("ROOK_B");
        models[14] = new JChessPiecesModel("BISHOP_B");
        models[15] = new JChessPiecesModel("KNIGHT_B");
        models[16] = new JChessPiecesModel("LANCE_B");
        models[17] = new JChessPiecesModel("PAWN_B");
        models[18] = new JChessPiecesModel("SILVERP_B");
        models[19] = new JChessPiecesModel("KNIGHTP_B");
        models[20] = new JChessPiecesModel("LANCEP_B");
        models[21] = new JChessPiecesModel("PAWNP_B");
        models[22] = new JChessPiecesModel("DRAGON_B");
        models[23] = new JChessPiecesModel("HORSE_B");

        models[24] = new JChessPiecesModel("KING_W");
        models[25] = new JChessPiecesModel("GOLD_W");
        models[26] = new JChessPiecesModel("SILVER_W");
        models[27] = new JChessPiecesModel("ROOK_W");
        models[28] = new JChessPiecesModel("BISHOP_W");
        models[29] = new JChessPiecesModel("KNIGHT_W");
        models[30] = new JChessPiecesModel("LANCE_W");
        models[31] = new JChessPiecesModel("PAWN_W");
        models[32] = new JChessPiecesModel("SILVERP_W");
        models[33] = new JChessPiecesModel("KNIGHTP_W");
        models[34] = new JChessPiecesModel("LANCEP_W");
        models[35] = new JChessPiecesModel("PAWNP_W");
        models[36] = new JChessPiecesModel("DRAGON_W");
        models[37] = new JChessPiecesModel("HORSE_W");


        return models;
    }

    public static JChessPiecesModel getSelectedModel() {
        return new JChessPiecesModel("SELECTED");
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.scale(0.85f, 0.85f, 0.85f);
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

}
