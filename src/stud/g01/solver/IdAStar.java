package stud.g01.solver;

import core.problem.Problem;
import core.solver.algorithm.heuristic.Predictor;
import core.solver.algorithm.searcher.AbstractSearcher;
import core.solver.queue.Frontier;
import core.solver.queue.Node;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 迭代加深 A* (IDA*) 算法实现。
 * 这是一个深度优先搜索，使用 f(n) = g(n) + h(n) 作为界限（bound）进行迭代。
 * 它解决了 A* 算法的指数级内存占用的问题。
 */
public class IdAStar extends AbstractSearcher {

    private final Predictor predictor;
    private Node solutionNode; // 用于在递归中存储找到的目标节点
    private int minNextBound;  // 用于存储下一轮迭代的界限
    private final Deque<Node> path; // 用于在 DFS 中存储当前路径，以进行循环检测

    /**
     * 构造函数
     * @param frontier  (在此算法中未使用，但为 API 兼容性保留)
     * @param predictor 启发式函数
     */
    public IdAStar(Frontier frontier, Predictor predictor) {
        super(frontier); // frontier 字段在此处未被积极使用
        this.predictor = predictor;
        this.path = new ArrayDeque<>();
    }

    @Override
    public Deque<Node> search(Problem problem) {
        // 重置计数器
        this.nodesGenerated = 0;
        this.nodesExpanded = 0;

        Node root = problem.root(predictor);
        this.nodesGenerated = 1;
        int bound = root.evaluation(); // 初始界限 = f(root)

        path.clear();
        path.push(root); // DFS 路径栈

        while (true) {
            // System.out.println("Searching with bound: " + bound); // 调试信息
            this.minNextBound = Integer.MAX_VALUE;
            this.solutionNode = null;

            // 开始递归搜索
            searchRecursive(problem, bound);

            if (this.solutionNode != null) {
                // 找到了！
                return generatePath(this.solutionNode);
            }
            if (this.minNextBound == Integer.MAX_VALUE) {
                // 搜索已穷尽，且未找到解
                return null;
            }

            // 准备下一轮迭代
            bound = this.minNextBound;
        }
    }

    /**
     * IDA* 的核心递归深度优先搜索
     * @param problem 问题实例
     * @param bound   当前 f 值的界限
     */
    private void searchRecursive(Problem problem, int bound) {
        // 如果在其他递归分支中已找到解，则立即返回
        if (this.solutionNode != null) return;

        Node currentNode = path.peek(); // 查看当前路径的末端节点
        int f = currentNode.evaluation();

        if (f > bound) {
            // 此节点的 f 值已超界
            this.minNextBound = Math.min(this.minNextBound, f); // 记录这个 f 值，作为下一轮的候选界限
            return;
        }

        if (problem.goal(currentNode.getState())) {
            // 找到了目标！
            this.solutionNode = currentNode;
            return;
        }

        this.nodesExpanded++;

        // 扩展子节点
        for (Node child : problem.childNodes(currentNode, predictor)) {
            this.nodesGenerated++;

            // 关键：循环检测。如果子节点已在当前路径中，则跳过。
            if (!path.contains(child)) {
                path.push(child); // 进栈（深入一层）
                searchRecursive(problem, bound);
                path.pop();       // 出栈（回溯）

                // 如果已找到解，则停止扩展其他子节点并返回
                if (this.solutionNode != null) return;
            }
        }
    }
}