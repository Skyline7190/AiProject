package stud.g01.runner;

import core.problem.Problem;
import core.problem.State;
import core.runner.EngineFeeder;
import core.solver.algorithm.heuristic.HeuristicType;
import core.solver.algorithm.heuristic.Predictor;
import core.solver.queue.EvaluationType;
import core.solver.queue.Frontier;
import core.solver.queue.Node;
import stud.g01.problem.npuzzle.NPuzzleProblem;
import stud.g01.problem.npuzzle.PuzzleBoard;
import stud.g01.queue.PqFrontier; // 确保使用我们实现的 PqFrontier

import java.util.ArrayList;

/**
 * N-Puzzle 问题的 EngineFeeder。
 * 负责解析输入文件，创建 Problem 实例，并提供 Frontier 和 Predictor。
 */
public class PuzzleFeeder extends EngineFeeder {

    /**
     * 从字符串列表解析并生成 N-Puzzle 问题实例。
     * 预期格式 (每行): size initial_tile_1 ... initial_tile_N goal_tile_1 ... goal_tile_N
     * 例如: 3 8 6 7 2 5 4 3 0 1 1 2 3 4 5 6 7 8 0
     * @param problemLines 包含所有问题实例的字符串列表
     * @return Problem 实例列表
     */
    @Override
    public ArrayList<Problem> getProblems(ArrayList<String> problemLines) {
        ArrayList<Problem> problems = new ArrayList<>();

        for (String line : problemLines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split(" ");
            if (parts.length < 2) {
                continue;
            }

            try {
                int size = Integer.parseInt(parts[0]);
                int boardSize = size * size;

                // 检查数据是否完整
                if (parts.length < 1 + 2 * boardSize) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }

                // 解析初始状态
                int[][] initialBoard = new int[size][size];
                int k = 1; // parts[0] 是 size
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        initialBoard[i][j] = Integer.parseInt(parts[k++]);
                    }
                }
                State initialState = new PuzzleBoard(size, initialBoard);

                // 解析目标状态
                int[][] goalBoard = new int[size][size];
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        goalBoard[i][j] = Integer.parseInt(parts[k++]);
                    }
                }
                State goalState = new PuzzleBoard(size, goalBoard);

                // 创建并添加 Problem 实例
                problems.add(new NPuzzleProblem(initialState, goalState, size));

            } catch (NumberFormatException e) {
                System.err.println("Skipping line with invalid number: " + line);
            }
        }
        return problems;
    }

    /**
     * 返回 A* 算法所需的 Frontier (优先队列)
     * @param type 节点评估器类型 (f, g, h)
     * @return PqFrontier 实例
     */
    @Override
    public Frontier getFrontier(EvaluationType type) {
        // Node.evaluator(type) 返回一个 Comparator<Node>
        return new PqFrontier(Node.evaluator(type));
    }

    /**
     * 返回指定类型的启发式函数 (Predictor)
     * @param type 启发式函数类型 (MISPLACED, MANHATTAN)
     * @return Predictor 实例
     */
    @Override
    public Predictor getPredictor(HeuristicType type) {
        // 委托 PuzzleBoard 的静态工厂方法来创建
        return PuzzleBoard.predictor(type);
    }
}
