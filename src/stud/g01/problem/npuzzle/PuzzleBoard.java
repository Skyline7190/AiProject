package stud.g01.problem.npuzzle;

import core.problem.Action;
import core.problem.State;
import core.solver.algorithm.heuristic.HeuristicType;
import core.solver.algorithm.heuristic.Predictor;
import stud.g01.solver.PatternDatabase; // 导入 PDB 类

import java.util.*;

/**
 * N-Puzzle 问题的“状态”类。
 * (已修改为支持 PDB 和 Long 键)
 */
public class PuzzleBoard extends State {

    private final int size;
    private final int[][] board;
    private final int blankRow; // 空白格 '0' 的行号
    private final int blankCol; // 空白格 '0' 的列号

    // --- 缓存字段 ---
    private static Map<Integer, int[]> goalPositionsCache; // 用于曼哈顿距离
    private static PuzzleBoard cachedGoal;

    // --- PDB 缓存字段 ---
    private static PatternDatabase pdb1;
    private static PatternDatabase pdb2;
    private static PatternDatabase pdb3;
    private static boolean pdbsBuilt = false;
    private static PuzzleBoard pdbGoal = null;


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
        int maxDigits = String.valueOf(size * size - 1).length() + 1;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                // 对 "不关心" 的瓦片 (-1 或 15) 使用 '*'
                String s = (board[i][j] == -1 || board[i][j] == 0xF) ? "15" : String.valueOf(board[i][j]);
                System.out.printf("%-" + maxDigits + "s", s);
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

    /**
     * 检查在当前状态下，动作是否可用
     */
    public boolean isApplicable(Action action) {
        PuzzleMove move = (PuzzleMove) action;
        switch (move.getDirection()) {
            case UP:
                return blankRow > 0;
            case DOWN:
                return blankRow < size - 1;
            case LEFT:
                return blankCol > 0;
            case RIGHT:
                return blankCol < size - 1;
        }
        return false;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PuzzleBoard that = (PuzzleBoard) obj;
        // PDB build 依赖于 Arrays.deepEquals (如果不用Long键)
        // IdAStar 的 goal() 检查也依赖这个
        return this.size == that.size && Arrays.deepEquals(this.board, that.board);
    }

    @Override
    public int hashCode() {
        // PDB build 依赖于 Arrays.deepHashCode (如果不用Long键)
        return Arrays.deepHashCode(board);
    }

    // --- PDB 和 IdAStar 性能优化 ---

    /**
     * 将 *具体* 棋盘状态编码为 long
     * 用于 IdAStar 的 O(1) 循环检测 (pathSet)
     * @return 64位 long 编码
     */
    public long toLong() {
        long key = 0L;
        // 4x4 棋盘
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                // 先左移4位
                key <<= 4;
                // 或运算，添加当前瓦片
                key |= (long) this.board[i][j];
            }
        }
        return key;
    }

    /**
     * 为PDB "抽象" 一个棋盘并编码为 long
     * 用于 PDB 的 O(1) h值查询
     * @param pattern PDB 关心的瓦片
     * @return 64位 long 编码
     */
    public long toAbstractLong(Set<Integer> pattern) {
        long key = 0L;
        // 4x4 棋盘
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                // 先左移4位
                key <<= 4;

                int tile = this.board[i][j];
                long tileValue;

                if (tile == 0) {
                    tileValue = 0; // 空白
                } else if (pattern.contains(tile)) {
                    tileValue = tile; // 模式瓦片
                } else {
                    tileValue = 0xF; // 15 (不关心)
                }

                // 或运算
                key |= tileValue;
            }
        }
        return key;
    }

    // --- 启发式函数 (Heuristics) ---

    private void ensureCache(PuzzleBoard goal) {
        if (goalPositionsCache == null || !goal.equals(cachedGoal)) {
            goalPositionsCache = new HashMap<>();
            cachedGoal = goal;
            int goalSize = goal.getSize();
            for (int i = 0; i < goalSize; i++) {
                for (int j = 0; j < goalSize; j++) {
                    goalPositionsCache.put(goal.board[i][j], new int[]{i, j});
                }
            }
        }
    }

    /**
     * 确保 PDB 已经被构建。
     */
    private static synchronized void ensurePDBs(PuzzleBoard goal) {
        if (pdbsBuilt && goal.equals(pdbGoal)) {
            return;
        }

        if (goal.getSize() != 4) {
            // System.err.println("PDBs 仅为 4x4  puzzle 定义。"); // 3x3时保持安静
            pdbsBuilt = true;
            pdbGoal = goal;
            return;
        }

        System.out.println("正在构建 5-5-5 模式数据库 (Long 键)... 这可能需要 15-30 秒...");

        long startTime = System.currentTimeMillis();
        pdbGoal = goal;

        Set<Integer> p1_tiles = Set.of(1, 2, 3, 4, 5);
        pdb1 = new PatternDatabase(p1_tiles, goal);

        Set<Integer> p2_tiles = Set.of(6, 7, 8, 9, 10);
        pdb2 = new PatternDatabase(p2_tiles, goal);

        Set<Integer> p3_tiles = Set.of(11, 12, 13, 14, 15);
        pdb3 = new PatternDatabase(p3_tiles, goal);

        pdbsBuilt = true;
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("模式数据库构建完毕 (耗时 " + duration + " 秒)。");
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
        ensureCache(goal);

        int distance = 0;
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                int tile = this.board[i][j];
                if (tile != 0) {
                    if (goalPositionsCache.containsKey(tile)) {
                        int[] targetPos = goalPositionsCache.get(tile);
                        distance += Math.abs(i - targetPos[0]) + Math.abs(j - targetPos[1]);
                    }
                }
            }
        }
        return distance;
    }

    public int linearConflicts(PuzzleBoard goal) {
        ensureCache(goal);
        int conflicts = 0;
        // (省略... 保持你原有的 linearConflicts 方法)
        return conflicts * 2;
    }

    /**
     * 静态工厂方法：根据类型返回对应的启发式函数 (Predictor)
     *
     */
    public static Predictor predictor(HeuristicType type) {
        switch (type) {
            case MISPLACED:
                return (state, goal) -> ((PuzzleBoard) state).misplaced((PuzzleBoard) goal);
            case MANHATTAN:
                return (state, goal) -> ((PuzzleBoard) state).manhattan((PuzzleBoard) goal);
            case MANHATTAN_PLUS_LINEAR_CONFLICTS:
                return (state, goal) -> {
                    PuzzleBoard b = (PuzzleBoard) state;
                    PuzzleBoard g = (PuzzleBoard) goal;
                    return b.manhattan(g) + b.linearConflicts(g);
                };

            case DISJOINT_PATTERN:
                return (state, goal) -> {
                    PuzzleBoard goalBoard = (PuzzleBoard) goal;
                    ensurePDBs(goalBoard);

                    PuzzleBoard b = (PuzzleBoard) state;

                    if (b.getSize() != 4 || !pdbsBuilt || pdb1 == null) {
                        return b.manhattan(goalBoard);
                    }

                    // PDB_1 (1-5)
                    long key1 = b.toAbstractLong(pdb1.getPattern());
                    int h1 = pdb1.getHeuristic(key1);

                    // PDB_2 (6-10)
                    long key2 = b.toAbstractLong(pdb2.getPattern());
                    int h2 = pdb2.getHeuristic(key2);

                    // PDB_3 (11-15)
                    long key3 = b.toAbstractLong(pdb3.getPattern());
                    int h3 = pdb3.getHeuristic(key3);

                    return h1 + h2 + h3;
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

    public int[][] getBoard() {
        return board;
    }
}