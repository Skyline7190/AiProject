package stud.g01.queue;

import core.problem.State;
import core.solver.queue.Frontier;
import core.solver.queue.Node;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A* 算法使用的优先队列 Frontier。
 * 使用 Java 内置的 PriorityQueue，并额外使用一个 HashMap 来优化 contains 和 replace 操作。
 * 这是 A* 算法（图搜索）的标准实现。
 */
public class PqFrontier implements Frontier {

    private final PriorityQueue<Node> pq;
    private final Comparator<Node> evaluator;

    // 关键优化：使用 Map 跟踪已在 Frontier 中的状态及其对应的 Node
    // Key: State, Value: Node
    // 这允许 O(1) 复杂度的 contains 检查
    // 以及 O(logN) 复杂度的替换（remove + add）
    private final Map<State, Node> stateMap;

    /**
     * 构造函数
     * @param evaluator 节点比较器 (f = g + h)
     */
    public PqFrontier(Comparator<Node> evaluator) {
        this.evaluator = evaluator;
        this.pq = new PriorityQueue<>(evaluator);
        this.stateMap = new HashMap<>();
    }

    /**
     * 从队列头部取出一个 f 值最小的节点
     * @return f 值最小的节点
     */
    @Override
    public Node poll() {
        Node node = pq.poll();
        if (node != null) {
            stateMap.remove(node.getState());
        }
        return node;
    }

    /**
     * 清空队列
     */
    @Override
    public void clear() {
        pq.clear();
        stateMap.clear();
    }

    @Override
    public int size() {
        return pq.size();
    }

    @Override
    public boolean isEmpty() {
        return pq.isEmpty();
    }

    /**
     * 检查 Frontier 中是否已包含与 `node` 状态相同的节点
     * @param node 要检查的节点
     * @return true 如果包含
     */
    @Override
    public boolean contains(Node node) {
        return stateMap.containsKey(node.getState());
    }

    /**
     * 向 Frontier 中添加一个新节点。
     * 这是 A*（图搜索）算法的核心逻辑：
     * 1. 如果 `node` 的状态不在 Frontier 中：直接添加。
     * 2. 如果 `node` 的状态已在 Frontier 中（`oldNode`）：
     * 比较新旧节点的 f 值（或 g 值，取决于比较器）。
     * 如果新节点 `node` 更优，则用 `node` 替换 `oldNode`。
     * 否则，丢弃 `node`。
     * @param node 要插入的结点
     * @return true 如果成功插入或替换，false 如果被丢弃
     */
    @Override
    public boolean offer(Node node) {
        Node oldNode = stateMap.get(node.getState());

        if (oldNode == null) {
            // 状态不在 Frontier 中，直接添加
            pq.add(node);
            stateMap.put(node.getState(), node);
            return true;
        }

        // 状态已在 Frontier 中，比较新旧节点
        // evaluator.compare(old, new) > 0 表示 old > new，即 new 节点更优
        if (evaluator.compare(oldNode, node) > 0) {
            // 新节点更优，执行替换
            pq.remove(oldNode); // O(N) 或 O(logN) 取决于实现，但对于A*是必要的
            pq.add(node);

            stateMap.put(node.getState(), node); // 更新 map
            return true;
        }

        // 新节点不更优，丢弃
        return false;
    }
}
