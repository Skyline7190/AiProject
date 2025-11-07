package stud.g01.solver;

import core.problem.Problem;
import core.problem.State;
import core.solver.algorithm.heuristic.Predictor;
import core.solver.algorithm.searcher.AbstractSearcher;
import core.solver.queue.Frontier;
import core.solver.queue.Node;
import stud.g01.problem.npuzzle.PuzzleBoard; // 【新增】 导入 PuzzleBoard

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 迭代加深 A* (IDA*) 算法实现。
 * (已优化 O(1) 循环检测 - 使用 Long 键)
 */
public class IdAStar extends AbstractSearcher {

    private final Predictor predictor;
    private Node solutionNode;
    private int minNextBound;

    //  pathStack 仍用于回溯
    private final Deque<Node> pathStack;
    //  pathSet 现在使用 Long 键进行真正的 O(1) 检测
    private final Set<Long> pathSet;


    /**
     * 构造函数
     * @param frontier  (在此算法中未使用，但为 API 兼容性保留)
     * @param predictor 启发式函数
     */
    public IdAStar(Frontier frontier, Predictor predictor) {
        super(frontier);
        this.predictor = predictor;

        this.pathStack = new ArrayDeque<>();
        //  初始化为 HashSet<Long>
        this.pathSet = new HashSet<>();
    }

    @Override
    public Deque<Node> search(Problem problem) {
        this.nodesGenerated = 0;
        this.nodesExpanded = 0;

        Node root = problem.root(predictor);
        this.nodesGenerated = 1;
        int bound = root.evaluation();

        pathStack.clear();
        pathSet.clear();

        // 添加 Long 键
        // 确保根状态是 PuzzleBoard
        if (root.getState() instanceof PuzzleBoard) {
            pathStack.push(root);
            pathSet.add(((PuzzleBoard)root.getState()).toLong()); // 使用 toLong()
        } else {
            // 处理非 PuzzleBoard 问题的回退（虽然在此项目中不必要）
            pathStack.push(root);
        }


        while (true) {
            // *** 取消注释这一行! ***
            System.out.println("Searching with bound: " + bound + " (已生成: " + this.nodesGenerated + " 节点)");

            this.minNextBound = Integer.MAX_VALUE;
            this.solutionNode = null;

            searchRecursive(problem, bound);

            if (this.solutionNode != null) {
                return generatePath(this.solutionNode);
            }
            if (this.minNextBound == Integer.MAX_VALUE) {
                return null;
            }

            bound = this.minNextBound;
        }
    }

    /**
     * IDA* 的核心递归深度优先搜索
     * @param problem 问题实例
     * @param bound   当前 f 值的界限
     */
    private void searchRecursive(Problem problem, int bound) {
        if (this.solutionNode != null) return;

        Node currentNode = pathStack.peek();
        int f = currentNode.evaluation();

        if (f > bound) {
            this.minNextBound = Math.min(this.minNextBound, f);
            return;
        }

        if (problem.goal(currentNode.getState())) {
            this.solutionNode = currentNode;
            return;
        }

        this.nodesExpanded++;

        // 扩展子节点
        for (Node child : problem.childNodes(currentNode, predictor)) {
            this.nodesGenerated++;

            // 关键：使用 Long 键进行 O(1) 检查
            long childKey = ((PuzzleBoard)child.getState()).toLong();

            if (!pathSet.contains(childKey)) {

                // 添加 Long 键
                pathStack.push(child);
                pathSet.add(childKey);

                searchRecursive(problem, bound);

                // 移除 Long 键
                // (注意：我们从 pathStack 中 pop，然后用它来获取 key)
                pathSet.remove(((PuzzleBoard)pathStack.pop().getState()).toLong());

                if (this.solutionNode != null) return;
            }
        }
    }
}