package stud.g01.problem.npuzzle;

import core.problem.Action;

/**
 * N-Puzzle 问题的“动作”类。
 * 代表空白格（0）可以移动的方向。
 */
public class PuzzleMove extends Action {

    /**
     * 枚举类，定义了四个可能的移动方向
     */
    public enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    private final Direction direction;

    /**
     * 构造函数
     * @param direction 移动方向 (UP, DOWN, LEFT, RIGHT)
     */
    public PuzzleMove(Direction direction) {
        this.direction = direction;
    }

    /**
     * 获取移动方向
     * @return 移动方向
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * 在控制台打印移动动作（调试用）
     */
    @Override
    public void draw() {
        System.out.println(this);
    }

    /**
     * N-Puzzle 问题中，每一步的代价固定为 1
     * @return 步骤代价 (1)
     */
    @Override
    public int stepCost() {
        return 1;
    }

    /**
     * 动作的字符串表示
     * @return "UP", "DOWN", "LEFT", "RIGHT"
     */
    @Override
    public String toString() {
        return direction.name();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PuzzleMove that = (PuzzleMove) obj;
        return direction == that.direction;
    }

    @Override
    public int hashCode() {
        return direction.hashCode();
    }
}
