package com.github.sangeeeee.tlm_shogi.client.gui.game;

import com.github.sangeeeee.tlm_shogi.network.message.JChessPromoteResultPackage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class JChessPromoteScreen extends Screen {

    private static final Component TITLE = Component.translatable("gui.tlm_shogi.jchess.promote");
    private static final Component YES = Component.translatable("gui.yes");
    private static final Component NO = Component.translatable("gui.no");

    private final BlockPos chessPos;
    private final String expectedSfen;
    private final int fromPos;
    private final int toPos;

    public JChessPromoteScreen(BlockPos chessPos, String expectedSfen, int fromPos, int toPos) {
        super(TITLE);
        this.chessPos = chessPos;
        this.expectedSfen = expectedSfen;
        this.fromPos = fromPos;
        this.toPos = toPos;
    }

    @Override
    protected void init() {
        int w = 150;
        int x = (this.width - w) / 2;
        int y = this.height / 4;

        this.addRenderableWidget(Button.builder(YES, btn -> {
            PacketDistributor.sendToServer(new JChessPromoteResultPackage(
                    chessPos, expectedSfen, fromPos, toPos, 1));
            this.onClose();
        }).pos(x + 15, y + 25).size(50, 20).build());

        this.addRenderableWidget(Button.builder(NO, btn -> {
            PacketDistributor.sendToServer(new JChessPromoteResultPackage(
                    chessPos, expectedSfen, fromPos, toPos, 2));
            this.onClose();
        }).pos(x + w - 65, y + 25).size(50, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        // 没点到按钮 → 取消
        PacketDistributor.sendToServer(new JChessPromoteResultPackage(
                chessPos, expectedSfen, fromPos, toPos, 0));
        this.onClose();
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mx, int my, float pt) {
        this.renderBackground(gfx, mx, my, pt);
        gfx.drawCenteredString(this.font, TITLE, this.width / 2, this.height / 4 - 10, 0xFFFFFF);
        for (Renderable renderable : this.renderables) {
            renderable.render(gfx, mx, my, pt);
        }
    }

    @Override public boolean isPauseScreen() { return false; } // 不暂停游戏
}
