package stud.g01.solver;

import core.problem.Problem;
import core.solver.algorithm.searcher.AbstractSearcher;
import core.solver.queue.Frontier;
import core.solver.queue.Node;
import core.solver.algorithm.heuristic.Predictor;//引入 Predictor

import java.util.Deque;

public class IdAStar extends AbstractSearcher {

    private final Predictor predictor;
    /**
     * 修改构造函数，让它接收 Frontier 和 Predictor 两个参数
     * @param frontier
     * @param predictor
     */
    public IdAStar(Frontier frontier, Predictor predictor) {
        super(frontier);
        this.predictor = predictor;
    }

    @Override
    public Deque<Node> search(Problem problem) {
        return null;
    }
}
