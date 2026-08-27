package com.github.sangeeeee.tlm_shogi.engine.core;

/** The twelve sliding, stepping, and knight directions used by Sunfish. */
public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    LEFT_UP,
    LEFT_DOWN,
    RIGHT_UP,
    RIGHT_DOWN,
    LEFT_UP_KNIGHT,
    LEFT_DOWN_KNIGHT,
    RIGHT_UP_KNIGHT,
    RIGHT_DOWN_KNIGHT,
    NONE;

    public Direction reversed() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case LEFT_UP -> RIGHT_DOWN;
            case LEFT_DOWN -> RIGHT_UP;
            case RIGHT_UP -> LEFT_DOWN;
            case RIGHT_DOWN -> LEFT_UP;
            case LEFT_UP_KNIGHT -> RIGHT_DOWN_KNIGHT;
            case LEFT_DOWN_KNIGHT -> RIGHT_UP_KNIGHT;
            case RIGHT_UP_KNIGHT -> LEFT_DOWN_KNIGHT;
            case RIGHT_DOWN_KNIGHT -> LEFT_UP_KNIGHT;
            case NONE -> NONE;
        };
    }

    public Direction horizontalSymmetry() {
        return switch (this) {
            case UP -> UP;
            case DOWN -> DOWN;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case LEFT_UP -> RIGHT_UP;
            case LEFT_DOWN -> RIGHT_DOWN;
            case RIGHT_UP -> LEFT_UP;
            case RIGHT_DOWN -> LEFT_DOWN;
            case LEFT_UP_KNIGHT -> RIGHT_UP_KNIGHT;
            case LEFT_DOWN_KNIGHT -> RIGHT_DOWN_KNIGHT;
            case RIGHT_UP_KNIGHT -> LEFT_UP_KNIGHT;
            case RIGHT_DOWN_KNIGHT -> LEFT_DOWN_KNIGHT;
            case NONE -> NONE;
        };
    }
}
