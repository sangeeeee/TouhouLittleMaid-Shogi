package com.github.tartaricacid.touhoulittlemaid.block.properties;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public enum ShogiPart implements StringRepresentable {
    CENTER("center", 0, 0),
    LEFT_CENTER_NS("left_ns", -1, 0),
    RIGHT_CENTER_NS("right_ns", 1, 0),
    LEFT_CENTER_EW("left_ew", 0, 1),
    RIGHT_CENTER_EW("right_ew", 0, -1);

    private final String name;
    private final int posX;
    private final int posY;

    ShogiPart(String name, int posX, int posY) {
        this.name = name;
        this.posX = posX;
        this.posY = posY;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public String toString() {
        return String.format("%s[%d, %d]", this.getSerializedName(), this.posX, this.posY);
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    public boolean isCenter() {
        return this == CENTER;
    }

    @Nullable
    public static ShogiPart getPartByPos(int x, int y) {
        for (ShogiPart part : ShogiPart.values()) {
            if (part.getPosX() == x && part.getPosY() == y) {
                return part;
            }
        }
        return null;
    }
}
