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

    // --- 缓存字段 ---
    private static Map<Integer, int[]> goalPositionsCache;
    private static PuzzleBoard cachedGoal;

    /**
     * 构造函数
     * @param size N-Puzzle 的边长 (例如 8-Puzzle 为 3)
     * @param board 二维数组表示的棋盘
     */
    public PuzzleBoard(int size, int[][] board) {
        this.size = size;
        this.board = board;

        int[] blankPos = findBlank(board, size);
        this.blankRow = blankPos[0];
        this.blankCol = blankPos[1];
    }

    private int[] findBlank(int[][] board, int size) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{-1, -1};
    }

    @Override
    public void draw() {
        System.out.println("-------");
        // 使用制表符 \t 来对齐，确保 15-Puzzle 也能对齐
        int maxDigits = String.valueOf(size * size - 1).length() + 1;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.printf("%-" + maxDigits + "s", board[i][j]);
            }
            System.out.println();
        }
        System.out.println("-------");
    }

    @Override
    public State next(Action action) {
        PuzzleMove move = (PuzzleMove) action;
        int newRow = blankRow, newCol = blankCol;

        switch (move.getDirection()) {
            case UP:    newRow--; break;
            case DOWN:  newRow++; break;
            case LEFT:  newCol--; break;
            case RIGHT: newCol++; break;
        }

        int[][] newBoard = new int[size][];
        for (int i = 0; i < size; i++) {
            newBoard[i] = Arrays.copyOf(board[i], size);
        }

        newBoard[blankRow][blankCol] = newBoard[newRow][newCol];
        newBoard[newRow][newCol] = 0;

        return new PuzzleBoard(size, newBoard);
    }

    @Override
    public Iterable<? extends Action> actions() {
        List<Action> moves = new ArrayList<>();
        moves.add(new PuzzleMove(PuzzleMove.Direction.UP));
        moves.add(new PuzzleMove(PuzzleMove.Direction.DOWN));
        moves.add(new PuzzleMove(PuzzleMove.Direction.LEFT));
        moves.add(new PuzzleMove(PuzzleMove.Direction.RIGHT));
        return moves;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PuzzleBoard that = (PuzzleBoard) obj;
        if (this.size != that.size || this.blankRow != that.blankRow || this.blankCol != that.blankCol) {
            return false;
        }
        return Arrays.deepEquals(this.board, that.board);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }

    // --- 启发式函数 (Heuristics) ---

    /**
     * 确保目标位置的缓存已经为 'goal' 棋盘构建。
     * @param goal 目标状态
     */
    private void ensureCache(PuzzleBoard goal) {
        // 检查缓存是否为空，或者是否是为 *不同* 的目标（例如, 从 8-puzzle 切换到 15-puzzle）
        if (goalPositionsCache == null || !goal.equals(cachedGoal)) {
            // 如果是新目标，重建缓存
            goalPositionsCache = new HashMap<>();
            cachedGoal = goal; // 记住我们是为这个目标构建的缓存
            int goalSize = goal.getSize(); // 从 goal 获取大小

            for (int i = 0; i < goalSize; i++) {
                for (int j = 0; j < goalSize; j++) {
                    goalPositionsCache.put(goal.board[i][j], new int[]{i, j});
                }
            }
        }
    }

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

    public int manhattan(PuzzleBoard goal) {
        ensureCache(goal); // 确保缓存已为这个 goal 构建

        int distance = 0;
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
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
     * 启发式函数 3：线性冲突 (Linear Conflicts)
     * @param goal 目标状态
     * @return 冲突数 * 2
     */
    public int linearConflicts(PuzzleBoard goal) {
        ensureCache(goal); // 依赖于 manhattan 方法先建立的缓存
        int conflicts = 0;

        // 检查行冲突
        for (int r = 0; r < size; r++) {
            for (int c1 = 0; c1 < size; c1++) {
                int tile1 = this.board[r][c1];
                if (tile1 == 0) continue;

                int[] goalPos1 = goalPositionsCache.get(tile1);
                if (goalPos1[0] != r) continue; // tile1 不在它的目标行

                for (int c2 = c1 + 1; c2 < size; c2++) {
                    int tile2 = this.board[r][c2];
                    if (tile2 == 0) continue;

                    int[] goalPos2 = goalPositionsCache.get(tile2);
                    if (goalPos2[0] != r) continue; // tile2 不在它的目标行

                    // 两个 tile 都在它们的目标行
                    // 检查它们是否颠倒了
                    if (goalPos1[1] > goalPos2[1]) { // tile1 的目标在 tile2 的右边
                        // 但 tile1 (c1) 当前在 tile2 (c2) 的左边
                        conflicts++;
                    }
                }
            }
        }

        // 检查列冲突 (逻辑相同，交换 r 和 c)
        for (int c = 0; c < size; c++) {
            for (int r1 = 0; r1 < size; r1++) {
                int tile1 = this.board[r1][c];
                if (tile1 == 0) continue;

                int[] goalPos1 = goalPositionsCache.get(tile1);
                if (goalPos1[1] != c) continue; // tile1 不在它的目标列

                for (int r2 = r1 + 1; r2 < size; r2++) {
                    int tile2 = this.board[r2][c];
                    if (tile2 == 0) continue;

                    int[] goalPos2 = goalPositionsCache.get(tile2);
                    if (goalPos2[1] != c) continue; // tile2 不在它的目标列

                    // 两个 tile 都在它们的目标列
                    if (goalPos1[0] > goalPos2[0]) { // tile1 的目标在 tile2 的下边
                        // 但 tile1 (r1) 当前在 tile2 (r2) 的上边
                        conflicts++;
                    }
                }
            }
        }

        return conflicts * 2; // 每次冲突至少需要 2 次额外移动来解决
    }

    /**
     * 静态工厂方法：根据类型返回对应的启发式函数 (Predictor)
     */
    public static Predictor predictor(HeuristicType type) {
        switch (type) {
            case MISPLACED:
                return (state, goal) -> ((PuzzleBoard) state).misplaced((PuzzleBoard) goal);
            case MANHATTAN:
                return (state, goal) -> ((PuzzleBoard) state).manhattan((PuzzleBoard) goal);
            case MANHATTAN_PLUS_LINEAR_CONFLICTS:
                // 这是我们新的、更强大的启发函数
                return (state, goal) -> {
                    PuzzleBoard b = (PuzzleBoard) state;
                    PuzzleBoard g = (PuzzleBoard) goal;
                    return b.manhattan(g) + b.linearConflicts(g);
                };
            default:
                return (state, goal) -> 0;
        }
    }

    // --- 可解性 (Solvability) ---

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
