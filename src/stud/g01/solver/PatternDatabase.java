package stud.g01.solver;

import core.problem.Action;
import stud.g01.problem.npuzzle.PuzzleBoard;
import stud.g01.problem.npuzzle.PuzzleMove;

import java.util.*;

/**
 * 模式数据库 (PDB) 实现。
 *
 */
public class PatternDatabase {

    private final Map<Long, Byte> database;
    private final Set<Integer> pattern;
    private final int size;

    // 预先计算 4x4 棋盘上每个位置的 'long' 键掩码 (mask)
    private static final long[][] MASKS = new long[4][4];
    // 预先计算每个位置的 'long' 键位移
    private static final int[][] SHIFTS = new int[4][4];

    static {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int shift = (15 - (i * 4 + j)) * 4;
                SHIFTS[i][j] = shift;
                MASKS[i][j] = 0xFL << shift;
            }
        }
    }

    public PatternDatabase(Set<Integer> pattern, PuzzleBoard goal) {
        this.pattern = pattern;
        this.size = goal.getSize();
        this.database = new HashMap<>();
        build(goal);
    }

    /**
     *  使用 BFS 从目标状态反向构建 PDB (直接操作 Long 键)
     */
    private void build(PuzzleBoard goal) {
        // 队列存储 <Long 键, 空白格行, 空白格列>
        Queue<long[]> queue = new ArrayDeque<>();

        // 1. 获取目标板 (e.g., 1 2 3 ... 0)
        long goalKey = goal.toAbstractLong(this.pattern);
        int[] blankPos = findTile(goalKey, 0); // 找到 '0' (空白格) 的位置

        // 2. 将 (Long 键, 0步) 添加到数据库
        database.put(goalKey, (byte) 0);
        // 3. 将 (Long 键, 空白格行, 空白格列) 添加到队列
        queue.add(new long[]{goalKey, blankPos[0], blankPos[1]});

        int maxDepth = 0;
        long startTime = System.currentTimeMillis();

        while (!queue.isEmpty()) {
            long[] current = queue.poll();
            long currentKey = current[0];
            int currentRow = (int) current[1];
            int currentCol = (int) current[2];
            byte currentCost = database.get(currentKey);

            if (currentCost > maxDepth) {
                maxDepth = currentCost;
                //System.out.println("PDB " + pattern + " building... Depth: " + maxDepth + ", States: " + database.size());
            }

            // 模拟四个方向的移动
            for (PuzzleMove.Direction dir : PuzzleMove.Direction.values()) {
                int newRow = currentRow, newCol = currentCol;
                switch (dir) {
                    case UP:    newRow--; break;
                    case DOWN:  newRow++; break;
                    case LEFT:  newCol--; break;
                    case RIGHT: newCol++; break;
                }

                // 检查是否越界
                if (newRow >= 0 && newRow < size && newCol >= 0 && newCol < size) {
                    // 模拟移动，生成新的 long 键
                    long childKey = swapTiles(currentKey, currentRow, currentCol, newRow, newCol);

                    if (!database.containsKey(childKey)) {
                        database.put(childKey, (byte) (currentCost + 1));
                        queue.add(new long[]{childKey, newRow, newCol});
                    }
                }
            }
        }
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("PDB " + pattern + " build complete. Total states: " + database.size() + ", Max depth: " + maxDepth + ", Time: " + duration + "s");
    }

    /**
     * 辅助方法：在 long 键上找到特定瓦片 (tile) 的 [row, col]
     */
    private int[] findTile(long key, int tileToFind) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int tile = (int) ((key & MASKS[i][j]) >>> SHIFTS[i][j]);
                if (tile == tileToFind) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{-1, -1}; // 理论上对于 '0' 不会发生
    }

    /**
     * 辅助方法：在 long 键上交换两个位置的瓦片，返回新键
     */
    private long swapTiles(long key, int r1, int c1, int r2, int c2) {
        // 1. 获取两个位置的瓦片值
        long tile1 = (key & MASKS[r1][c1]) >>> SHIFTS[r1][c1]; // e.g., '0'
        long tile2 = (key & MASKS[r2][c2]) >>> SHIFTS[r2][c2]; // e.g., 'F' or '5'

        // 2. 清除这两个位置
        key &= ~MASKS[r1][c1];
        key &= ~MASKS[r2][c2];

        // 3. 将瓦片值交换并放回
        key |= (tile1 << SHIFTS[r2][c2]);
        key |= (tile2 << SHIFTS[r1][c1]);

        return key;
    }


    /**
     * 从数据库中获取启发值 (O(1) 查询)
     */
    public int getHeuristic(long key) {
        return database.getOrDefault(key, (byte) 0);
    }

    /**
     * 允许 PuzzleBoard 获取模式集
     */
    public Set<Integer> getPattern() {
        return this.pattern;
    }
}