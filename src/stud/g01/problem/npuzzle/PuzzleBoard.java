package stud.g01.problem.npuzzle;

import core.problem.Action;
import core.problem.State;
import core.solver.algorithm.heuristic.HeuristicType;
import core.solver.algorithm.heuristic.Predictor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * N-Puzzle 问题的“状态”类。
 * 封装了 N-Puzzle 的棋盘信息 (board) 和相关操作。
 */
public class PuzzleBoard extends State {

    private final int size;
    private final int[][] board;
    private final int blankRow; // 空白格 '0' 的行号
    private final int blankCol; // 空白格 '0' 的列号

    // 用于快速查找曼哈顿距离的目标位置
    private static Map<Integer, int[]> goalPositionsCache;

    /**
     * 构造函数
     * @param size N-Puzzle 的边长 (例如 8-Puzzle 为 3)
     * @param board 二维数组表示的棋盘
     */
    public PuzzleBoard(int size, int[][] board) {
        this.size = size;
        this.board = board;

        // 寻找空白格 '0' 的位置
        int[] blankPos = findBlank(board, size);
        this.blankRow = blankPos[0];
        this.blankCol = blankPos[1];
    }

    /**
     * 辅助函数：寻找空白格 '0'
     */
    private int[] findBlank(int[][] board, int size) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{-1, -1}; // 理论上不会发生
    }

    /**
     * 在控制台打印当前棋盘状态
     */
    @Override
    public void draw() {
        System.out.println("-------");
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("-------");
    }

    /**
     * 根据传入的动作（Move），生成并返回一个新的状态（PuzzleBoard）
     * @param action 要执行的动作 (PuzzleMove)
     * @return 执行动作后的新状态
     */
    @Override
    public State next(Action action) {
        PuzzleMove move = (PuzzleMove) action;
        int newRow = blankRow, newCol = blankCol;

        // 计算空白格的新位置
        switch (move.getDirection()) {
            case UP:    newRow--; break;
            case DOWN:  newRow++; break;
            case LEFT:  newCol--; break;
            case RIGHT: newCol++; break;
        }

        // 复制当前棋盘到新棋盘
        int[][] newBoard = new int[size][];
        for (int i = 0; i < size; i++) {
            newBoard[i] = Arrays.copyOf(board[i], size);
        }

        // 在新棋盘上交换方块
        newBoard[blankRow][blankCol] = newBoard[newRow][newCol];
        newBoard[newRow][newCol] = 0; // '0' 移动到新位置

        return new PuzzleBoard(size, newBoard);
    }

    /**
     * 返回所有可能的动作（上、下、左、右）
     * 注意：这里返回所有4个方向，具体的“适用性”检查由 NPuzzleProblem.applicable() 完成
     * @return 包含4个方向的 PuzzleMove 列表
     */
    @Override
    public Iterable<? extends Action> actions() {
        List<Action> moves = new ArrayList<>();
        moves.add(new PuzzleMove(PuzzleMove.Direction.UP));
        moves.add(new PuzzleMove(PuzzleMove.Direction.DOWN));
        moves.add(new PuzzleMove(PuzzleMove.Direction.LEFT));
        moves.add(new PuzzleMove(PuzzleMove.Direction.RIGHT));
        return moves;
    }

    // --- 核心方法：equals 和 hashCode ---

    /**
     * 比较两个 PuzzleBoard 状态是否相同
     * @param obj 另一个对象
     * @return 如果棋盘布局完全相同，则为 true
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PuzzleBoard that = (PuzzleBoard) obj;
        return Arrays.deepEquals(this.board, that.board);
    }

    /**
     * 为当前棋盘状态生成哈希码
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }

    // --- 启发式函数 (Heuristics) ---

    /**
     * 启发式函数 1：错位将牌数 (Misplaced Tiles)
     * @param goal 目标状态
     * @return 不在目标位置的方块数量（不包括空白格）
     */
    public int misplaced(PuzzleBoard goal) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (this.board[i][j] != 0 && this.board[i][j] != goal.board[i][j]) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 启发式函数 2：曼哈顿距离 (Manhattan Distance)
     * @param goal 目标状态
     * @return 所有方块到其目标位置的曼哈顿距离之和
     */
    public int manhattan(PuzzleBoard goal) {
        // 懒加载并缓存目标位置
        if (goalPositionsCache == null) {
            goalPositionsCache = new HashMap<>();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    goalPositionsCache.put(goal.board[i][j], new int[]{i, j});
                }
            }
        }

        int distance = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int tile = this.board[i][j];
                if (tile != 0) {
                    int[] targetPos = goalPositionsCache.get(tile);
                    distance += Math.abs(i - targetPos[0]) + Math.abs(j - targetPos[1]);
                }
            }
        }
        return distance;
    }

    /**
     * 静态工厂方法：根据类型返回对应的启发式函数 (Predictor)
     * @param type 启发式函数类型 (MISPLACED, MANHATTAN)
     * @return 实现了 Predictor 接口的 Lambda 表达式
     */
    public static Predictor predictor(HeuristicType type) {
        switch (type) {
            case MISPLACED:
                // state 和 goal 会在搜索时由 A* 算法传入
                return (state, goal) -> ((PuzzleBoard) state).misplaced((PuzzleBoard) goal);
            case MANHATTAN:
                return (state, goal) -> ((PuzzleBoard) state).manhattan((PuzzleBoard) goal);
            default:
                // 默认返回 0
                return (state, goal) -> 0;
        }
    }

    // --- 可解性 (Solvability) ---

    /**
     * 计算逆序数
     * @return 逆序数
     */
    public int getInversions() {
        int inversions = 0;
        int[] flatBoard = new int[size * size];
        int k = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                flatBoard[k++] = board[i][j];
            }
        }

        for (int i = 0; i < flatBoard.length - 1; i++) {
            for (int j = i + 1; j < flatBoard.length; j++) {
                // 不计算空白格 '0'
                if (flatBoard[i] != 0 && flatBoard[j] != 0 && flatBoard[i] > flatBoard[j]) {
                    inversions++;
                }
            }
        }
        return inversions;
    }

    // --- Getters ---
    public int getSize() {
        return size;
    }

    public int getBlankRow() {
        return blankRow;
    }

    public int getBlankCol() {
        return blankCol;
    }
}
