package stud.g01.problem.npuzzle;

import core.problem.Action;
import core.problem.Problem;
import core.problem.State;
import core.solver.queue.Node;

import java.util.Deque;

/**
 * N-Puzzle 问题的“问题”类。
 * 负责定义：
 * 1. 动作的代价 (stepCost)
 * 2. 动作的适用性 (applicable)
 * 3. 问题的可解性 (solvable)
 * 4. 解法的展示 (showSolution)
 */
public class NPuzzleProblem extends Problem {

    public NPuzzleProblem(State initialState, State goal) {
        super(initialState, goal);
    }

    public NPuzzleProblem(State initialState, State goal, int size) {
        super(initialState, goal, size);
    }

    /**
     * 检查当前问题是否有解。
     * N-Puzzle 的可解性判断：
     * 1. 如果 N (size) 为奇数，逆序数必须为偶数。
     * 2. 如果 N (size) 为偶数，(逆序数 + 空白格所在行数（从底向上数，1-based）) 必须为奇数。
     * @return true 如果有解，false 如果无解
     */
    @Override
    public boolean solvable() {
        PuzzleBoard board = (PuzzleBoard) initialState;
        int inversions = board.getInversions();
        int n = board.getSize();

        if (n % 2 == 1) { // 奇数 N (例如 3x3)
            return (inversions % 2 == 0);
        } else { // 偶数 N (例如 4x4)
            // 空白格行号（从底向上数，1-based）
            int blankRowFromBottom = n - board.getBlankRow();
            return ((inversions + blankRowFromBottom) % 2 == 1);
        }
    }

    /**
     * N-Puzzle 每一步代价为 1
     * @param state  当前状态
     * @param action 采取的动作
     * @return 1
     */
    @Override
    public int stepCost(State state, Action action) {
        return 1; // 每一步代价都是 1
    }

    /**
     * 检查在
     * `state` 状态下，`action` 动作是否可用（即是否会移出棋盘边界）
     * @param state  当前状态
     * @param action 要检查的动作
     * @return true 如果可用，false 如果不可用
     */
    @Override
    public boolean applicable(State state, Action action) {
        PuzzleBoard board = (PuzzleBoard) state;
        PuzzleMove move = (PuzzleMove) action;
        int row = board.getBlankRow();
        int col = ((PuzzleBoard) state).getBlankCol(); // 需要在 PuzzleBoard 中添加 getBlankCol()
        int n = board.getSize();

        switch (move.getDirection()) {
            case UP:
                return row > 0; // 不能在顶行向上
            case DOWN:
                return row < n - 1; // 不能在底行向下
            case LEFT:
                return col > 0; // 不能在最左列向左
            case RIGHT:
                return col < n - 1; // 不能在最右列向右
        }
        return false;
    }

    /**
     * 打印解法路径
     * @param path A* 算法返回的节点路径
     */
    @Override
    public void showSolution(Deque<Node> path) {
        System.out.println("Solution Path:");
        // 初始状态
        System.out.println("Start State:");
        this.initialState.draw();

        int step = 1;
        for (Node node : path) {
            System.out.println("Step " + (step++) + ": Move " + node.getAction());
            node.getState().draw();
        }
        System.out.println("Goal Reached!");
    }
}
