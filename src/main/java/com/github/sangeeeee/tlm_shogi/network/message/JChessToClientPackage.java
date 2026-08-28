package com.github.sangeeeee.tlm_shogi.network.message;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.ShogiEngineInteractor;
import com.github.sangeeeee.tlm_shogi.mateengine.MateEngine;
import com.github.sangeeeee.tlm_shogi.mateengine.MateSearchLimits;
import com.github.sangeeeee.tlm_shogi.mateengine.MateSearchResult;
import com.github.sangeeeee.tlm_shogi.util.JChessUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil.getResourceLocation;

public record JChessToClientPackage(BlockPos pos, String fenData, boolean tsume,
                                    String scriptedMove) implements CustomPacketPayload {

    public static final Type<JChessToClientPackage> TYPE = new Type<>(getResourceLocation("jchess_to_client"));
    public static final StreamCodec<ByteBuf, JChessToClientPackage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            JChessToClientPackage::pos,
            ByteBufCodecs.STRING_UTF8,
            JChessToClientPackage::fenData,
            ByteBufCodecs.BOOL,
            JChessToClientPackage::tsume,
            ByteBufCodecs.STRING_UTF8,
            JChessToClientPackage::scriptedMove,
            JChessToClientPackage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JChessToClientPackage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> CompletableFuture.runAsync(() -> onHandle(message), Util.backgroundExecutor()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(JChessToClientPackage message) {
        int levelTime = 1000;
        long timeStart = System.currentTimeMillis();
        String move = "";

        if (message.tsume) {
            try {
                if (!message.scriptedMove.isBlank()) {
                    move = message.scriptedMove;
                } else {
                    MateSearchLimits limits = new MateSearchLimits(Duration.ofSeconds(3), 15, 500_000);
                    MateSearchResult result = new MateEngine().search(
                            message.fenData,
                            limits,
                            () -> Thread.currentThread().isInterrupted());
                    move = result.bestMove().orElseThrow(() ->
                            new IOException("Mate engine found no defensive move"));
                    TouhouLittleMaidShogi.LOGGER.debug(
                            "Tsume defense: outcome={}, move={}, matePlies={}, nodes={}, elapsed={} ms",
                            result.outcome(), move, result.matePlies(), result.nodes(), result.elapsed().toMillis());
                }
                waitForAnimation(timeStart, levelTime);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                move = "engine error";
            } catch (Exception exception) {
                TouhouLittleMaidShogi.LOGGER.error("Java mate-engine search failed", exception);
                move = "engine error";
            }

            final String moveFinal = move;
            Minecraft.getInstance().submitAsync(() -> PacketDistributor.sendToServer(
                    new JChessToServerPackage(message.pos, message.fenData, moveFinal, false, false)));
            return;
        }

        Position position = new Position();
        position.applyUSI(message.fenData);

        // 先判断玩家是否赢了
        // 是的，我放客户端，减轻服务端压力，理论上你可直接传布尔值判断女仆输掉来作弊
        // sange: 这河里吗，，，不管了那我也放这儿吧
        boolean maidLost = false;
        boolean playerLost = false;
        if (position.isCheck() && position.isMate() && JChessUtil.isMaid(position)) {
            maidLost = true;
        }

        if (!maidLost) {
            // TODO: 暂时不做女仆的棋技系统

            ShogiEngineInteractor interactor = new ShogiEngineInteractor();
            boolean setupCompleted = false;
            try {
                String json = "{\"USI_Hash\": \"64\", \"NodesLimit\": \"30000\", \"DepthLimit\": \"8\"}";
                interactor.setup(json);
                setupCompleted = true;
                move = interactor.interact(message.fenData, null);

                if (position.makeMove(move) < 0) {
                    throw new IOException("Java engine returned an invalid move: " + move);
                }
                if (position.isCheck() && position.isMate() && JChessUtil.isPlayer(position)) {
                    playerLost = true;
                }

                // 如果时间还有剩余，那么 sleep 一会儿
                waitForAnimation(timeStart, levelTime);
            } catch (IOException e) {
                TouhouLittleMaidShogi.LOGGER.error("Java shogi engine data or search failed", e);
                move = setupCompleted ? "engine error" : "no engine";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                TouhouLittleMaidShogi.LOGGER.warn("Java shogi engine search was interrupted", e);
                move = "engine error";
            } catch (Exception e) {
                TouhouLittleMaidShogi.LOGGER.error("Unexpected Java shogi engine error", e);
                move = "engine error";
            } finally {
                interactor.stop();
            }
        }

        final String moveFinal = move;
        final boolean playerLostFinal = playerLost;
        final boolean maidLostFinal = maidLost;
        Minecraft.getInstance().submitAsync(() -> PacketDistributor.sendToServer(
                new JChessToServerPackage(message.pos, message.fenData, moveFinal, maidLostFinal, playerLostFinal)));
    }

    private static void waitForAnimation(long timeStart, int minimumMillis) throws InterruptedException {
        long timeRemain = Math.max(0, minimumMillis - (System.currentTimeMillis() - timeStart));
        if (timeRemain > 0) {
            Thread.sleep(timeRemain);
        }
    }
}
