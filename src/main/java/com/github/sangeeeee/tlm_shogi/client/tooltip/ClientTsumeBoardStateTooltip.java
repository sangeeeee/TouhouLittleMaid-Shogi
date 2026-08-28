package com.github.sangeeeee.tlm_shogi.client.tooltip;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.api.game.jchess.Position;
import com.github.sangeeeee.tlm_shogi.inventory.tooltip.TsumeBoardStateTooltip;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Renders a 9x9 SFEN preview using the in-game board, blank koma, and 3D-piece glyph atlas. */
public final class ClientTsumeBoardStateTooltip implements ClientTooltipComponent {
    private static final ResourceLocation BOARD = ResourceLocation.fromNamespaceAndPath(
            TouhouLittleMaidShogi.MOD_ID, "textures/gui/jchess_board_state.png");
    private static final ResourceLocation PIECE = ResourceLocation.fromNamespaceAndPath(
            TouhouLittleMaidShogi.MOD_ID, "textures/gui/jchess_preview_piece.png");
    private static final ResourceLocation GLYPHS = ResourceLocation.fromNamespaceAndPath(
            TouhouLittleMaidShogi.MOD_ID, "textures/bedrock/block/jchess_pieces.png");
    private static final Function<String, PreviewData> CACHE = Util.memoize(ClientTsumeBoardStateTooltip::decode);

    private static final int BOARD_SIZE = 128;
    private static final int BOARD_TEXTURE_SIZE = 256;
    private static final int CELL_SIZE = 27;
    private static final int GRID_OFFSET = 6;
    private static final int PIECE_WIDTH = 22;
    private static final int PIECE_HEIGHT = 24;
    private static final int GLYPH_RENDER_WIDTH = 14;
    private static final int GLYPH_RENDER_HEIGHT = 15;
    private static final int TEXT_COLOR = 0xFFE8E8E8;

    private final PreviewData data;
    private final String playerHand;
    private final Component engineHand;

    public ClientTsumeBoardStateTooltip(TsumeBoardStateTooltip tooltip) {
        this.data = CACHE.apply(tooltip.sfen());
        this.playerHand = formatHand("☗", data.blackHand());
        this.engineHand = Component.translatable("tooltip.tlm_shogi.tsume.engine_hand_all");
    }

    private static PreviewData decode(String sfen) {
        try {
            Position position = new Position();
            position.applyUSI(sfen);
            int[][] board = new int[9][9];
            for (int y = 0; y < 9; y++) {
                for (int x = 0; x < 9; x++) {
                    board[y][x] = position.getPieceAt(x, y);
                }
            }
            return new PreviewData(true, board, copyHand(position.getBlackHand()));
        } catch (RuntimeException exception) {
            TouhouLittleMaidShogi.LOGGER.warn("Unable to render invalid tsume SFEN tooltip", exception);
            return PreviewData.invalid();
        }
    }

    private static List<int[]> copyHand(List<int[]> hand) {
        List<int[]> copy = new ArrayList<>(hand.size());
        for (int[] entry : hand) {
            copy.add(entry.clone());
        }
        return List.copyOf(copy);
    }

    @Override
    public int getHeight() {
        return data.valid() ? BOARD_SIZE + 22 : 0;
    }

    @Override
    public int getWidth(Font font) {
        if (!data.valid()) {
            return 0;
        }
        return Math.max(BOARD_SIZE, Math.max(font.width(playerHand), font.width(engineHand)));
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (!data.valid()) {
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(0.5f, 0.5f, 1.0f);
        graphics.blit(BOARD, 0, 0, 0, 0,
                BOARD_TEXTURE_SIZE, BOARD_TEXTURE_SIZE, BOARD_TEXTURE_SIZE, BOARD_TEXTURE_SIZE);

        for (int boardY = 0; boardY < 9; boardY++) {
            for (int boardX = 0; boardX < 9; boardX++) {
                int pieceId = data.board()[boardY][boardX];
                if (pieceId != 0) {
                    int pieceX = GRID_OFFSET + boardX * CELL_SIZE + 3;
                    int pieceY = GRID_OFFSET + boardY * CELL_SIZE + 2;
                    renderPiece(graphics, pose, pieceX, pieceY, pieceId);
                }
            }
        }
        pose.popPose();

        graphics.drawString(font, playerHand, x, y + BOARD_SIZE + 1, TEXT_COLOR, false);
        graphics.drawString(font, engineHand, x, y + BOARD_SIZE + 11, TEXT_COLOR, false);
    }

    private static void renderPiece(GuiGraphics graphics, PoseStack pose, int x, int y, int pieceId) {
        boolean enginePiece = pieceId >= 24;
        pose.pushPose();
        pose.translate(x, y, 0);
        if (enginePiece) {
            pose.translate(PIECE_WIDTH / 2.0f, PIECE_HEIGHT / 2.0f, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.translate(-PIECE_WIDTH / 2.0f, -PIECE_HEIGHT / 2.0f, 0);
        }

        pose.pushPose();
        pose.scale(PIECE_WIDTH / 128.0f, PIECE_HEIGHT / 128.0f, 1.0f);
        graphics.blit(PIECE, 0, 0, 0, 0, 128, 128, 128, 128);
        pose.popPose();

        Glyph glyph = Glyph.forPiece(pieceId);
        if (glyph != null) {
            pose.pushPose();
            // The white piece is rotated as a whole, so increasing this common Y
            // moves black down and white up by the same half preview pixel.
            pose.translate(4, 5, 0.1f);
            if (pieceId == 10) {
                // This atlas crop faces the opposite direction when used by the
                // unrotated player piece, so turn only the player's king glyph.
                pose.translate(GLYPH_RENDER_WIDTH / 2.0f, GLYPH_RENDER_HEIGHT / 2.0f, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.translate(-GLYPH_RENDER_WIDTH / 2.0f, -GLYPH_RENDER_HEIGHT / 2.0f, 0);
            }
            pose.scale(GLYPH_RENDER_WIDTH / (float) glyph.width(),
                    GLYPH_RENDER_HEIGHT / (float) glyph.height(), 1.0f);
            graphics.blit(GLYPHS, 0, 0, glyph.u(), glyph.v(),
                    glyph.width(), glyph.height(), 512, 512);
            pose.popPose();
        }
        pose.popPose();
    }

    private static String formatHand(String side, List<int[]> hand) {
        List<int[]> ordered = new ArrayList<>(hand);
        ordered.sort(Comparator.comparingInt(entry -> handOrder(entry[1])));
        StringBuilder text = new StringBuilder(side).append("持駒：");
        boolean any = false;
        for (int[] entry : ordered) {
            if (entry.length < 2 || entry[0] <= 0) {
                continue;
            }
            String name = handPieceName(entry[1]);
            if (name.isEmpty()) {
                continue;
            }
            if (any) {
                text.append(' ');
            }
            text.append(name);
            if (entry[0] > 1) {
                text.append(japaneseNumber(entry[0]));
            }
            any = true;
        }
        if (!any) {
            text.append("なし");
        }
        return text.toString();
    }

    private static int handOrder(int pieceId) {
        int normalized = pieceId >= 24 ? pieceId - 14 : pieceId;
        return switch (normalized) {
            case 13 -> 0; // 飛
            case 14 -> 1; // 角
            case 11 -> 2; // 金
            case 12 -> 3; // 銀
            case 15 -> 4; // 桂
            case 16 -> 5; // 香
            case 17 -> 6; // 歩
            default -> 99;
        };
    }

    private static String handPieceName(int pieceId) {
        int normalized = pieceId >= 24 ? pieceId - 14 : pieceId;
        return switch (normalized) {
            case 13 -> "飛";
            case 14 -> "角";
            case 11 -> "金";
            case 12 -> "銀";
            case 15 -> "桂";
            case 16 -> "香";
            case 17 -> "歩";
            default -> "";
        };
    }

    private static String japaneseNumber(int value) {
        String[] digits = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        if (value < 10) {
            return digits[Math.max(0, value)];
        }
        if (value < 20) {
            return "十" + digits[value - 10];
        }
        return Integer.toString(value);
    }

    private record PreviewData(boolean valid, int[][] board, List<int[]> blackHand) {
        static PreviewData invalid() {
            return new PreviewData(false, new int[9][9], List.of());
        }
    }

    private record Glyph(int u, int v, int width, int height) {
        private static Glyph forPiece(int pieceId) {
            int normalized = pieceId >= 24 ? pieceId - 14 : pieceId;
            return switch (normalized) {
                case 10 -> pieceId >= 24 ? new Glyph(132, 62, 37, 38) : new Glyph(133, 219, 37, 38);
                case 11 -> new Glyph(279, 63, 37, 38);
                case 12 -> new Glyph(327, 64, 37, 38);
                case 13 -> new Glyph(181, 63, 37, 38);
                case 14 -> new Glyph(229, 63, 37, 38);
                case 15 -> new Glyph(376, 63, 37, 38);
                case 16 -> new Glyph(424, 63, 37, 38);
                case 17 -> new Glyph(474, 64, 37, 38);
                case 18 -> new Glyph(328, 115, 37, 38);
                case 19 -> new Glyph(375, 116, 37, 38);
                case 20 -> new Glyph(424, 116, 37, 38);
                case 21 -> new Glyph(473, 115, 37, 38);
                case 22 -> new Glyph(181, 115, 37, 38);
                case 23 -> new Glyph(229, 115, 37, 38);
                default -> null;
            };
        }
    }
}
