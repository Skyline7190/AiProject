# AiProject

## 实验一

### 项目上手运行
拿到实验项目原封不动地运行N-Puzzle问题是会报错的。

初步分析项目框架：

1. 框架是完整的： 提供了所有核心的抽象类和接口，比如 `Problem`, `State`, `Action`, `AbstractSearcher` 等。
2. 提供了一个完整示例：寻路问题 (PATHFINDING) 的相关代码 (`stud.runner.WalkerFeeder`, `stud.problem.pathfinding.*`) 是完整且可以运行的。
3. 需要完成的部分：N-Puzzle问题 的核心逻辑是留空的，需要自己去填充代码。这就是为什么 `stud.g01.runner.PuzzleFeeder.java` 文件里的方法都返回 `null`

初步总结：

+ 能跑的部分： 如果您将IDEA运行配置中的参数设置为运行寻路问题（`PATHFINDING` 和 `stud.runner.WalkerFeeder`），程序可以正常运行。
+ 不能跑的部分： 当您尝试运行N-Puzzle问题（`NPUZZLE` 和 `stud.g01.runner.PuzzleFeeder`）时，程序不能运行，因为它依赖您去完成 `PuzzleFeeder` 以及其他 `stud.g01.*` 包下的代码实现。

实验任务就是参照寻路问题的实现，去完成N-Puzzle问题的代码。

#### 直接运行寻路问题
在运行的绿色三角左边点击“Current File”，在下拉菜单中找到“Edit Configurations”，配置如下

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1760668625784-be275d0e-2dc0-4ca8-950b-d57a9aaa1ada.png)

更改src/core/runner/EngineFeeder.java

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1760668988103-fb03811b-d437-4e40-b274-8616dc34f2a0.png)

更改src/stud/g01/solver/IdAStar.java

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1760669029017-4d887c8b-6903-468c-b9bf-3ac098b25416.png)

成功运行

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1760669196492-76b9f625-7a27-4a9d-b1ce-dbebe9bde556.png)

### 拆解项目结构
#### 主要目录
`src/core`：框架核心抽象（问题、节点、队列、搜索器、运行器）。

`src/stud`：具体示例与学生实现（拼图、寻路、不同 Frontiers、具体解算器）。

`resources`：输入问题文件（如 problems.txt、pathfinding.txt）。

`src/algs4`：通用工具（随机、计时器等）。

#### 主要模块
`core.problem`：

+ Problem：问题抽象（你打开的文件）。负责描述初始状态、目标状态、如何生成后继、步代价、是否可解、是否为目标、如何展示解。
+ State / Action（接口/抽象类，见同包）用于表示状态与动作。

`core.solver.queue`：

+ Node：搜索树节点，包含状态、父节点、动作、路径代价、启发值等。
+ Frontier：边界队列抽象，具体实现负责如何选择下一个展开节点（FIFO、LIFO、优先队列等）。
+ EvaluationType：优先比较的评估类型（例如 f=g+h，或仅 h 等）。

`core.solver.algorithm`：

+ AbstractSearcher：搜索器抽象，控制搜索流程（取节点、扩展、测试目标、处理边界）。
+ BestFirstSearcher：基于启发式/评价函数的搜索器实现。
+ heuristic 子包：Predictor 与 HeuristicType，用于对状态进行估值（h）。

`core.runner`：

+ EngineFeeder / SearchTester：负责把问题加载进来、配置搜索器与启发式、运行搜索并收集结果。

`stud`：**学生实现和实例**

+ 例如 stud/g01/problem/npuzzle 下的 NPuzzleProblem、PuzzleBoard 为 15 拼图实现；stud/problem/pathfinding 为寻路实现。
+ stud/*/queue 有 PqFrontier 等具体 Frontier。
+ stud/*/solver/IdAStar 是具体的解算器（例如迭代加深 A*）。

### 第一阶段
实现 8-Puzzle（3x3）的 A* 算法求解器，使用“错位将牌数”（Misplaced Tiles）和“曼哈顿距离”（Manhattan Distance）作为启发式函数

**目标与要点**

+ 实现 N-Puzzle 的 Action/State/Problem。
+ 实现启发函数：错位牌数与曼哈顿距离（默认使用曼哈顿）。
+ 实现 A*，带闭集与 g 值记录，路径重构。
+ 在 8-puzzle 常见实例上 1 秒内求解。

#### 实现 `Action` 接口
**代表“上、下、左、右”移动。**  

新增文件 src/stud/g01/problem/npuzzle/PuzzleMove.java

```java
package ...;//声明类所属包，决定编译后文件的包路径与访问控制。
import core.problem.Action;//引入父类 Action，本类通过继承使用其抽象方法/约定。
public class PuzzleMove extends Action {
//定义公共类 PuzzleMove，继承 Action，表示一个动作类型。
public enum Direction { UP, DOWN, LEFT, RIGHT }
//定义内部枚举 Direction，四个常量分别表示四个移动方向。
private final Direction direction;
//声明不可变字段 direction，保存该动作的方向。
public PuzzleMove(Direction direction) { this.direction = direction; }
//构造函数：接收一个 Direction，直接赋值给不可变字段（未做 null 校验）。
public Direction getDirection() { return direction; }//访问器：返回动作的方向。
@Override public void draw() { System.out.println(this); }
//覆写 draw()：在控制台打印当前对象（调用 toString() 的结果）。
@Override public int stepCost() { return 1; }
//覆写 stepCost()：每一步的代价固定为 1（用于搜索算法累加路径代价）。
@Override public String toString() { return direction.name(); }
//覆写 toString()：返回方向的名字（"UP"/"DOWN"/"LEFT"/"RIGHT"），便于日志和调试。
equals 实现：
if (this == obj) return true; 
//—— 相同引用直接相等。
if (obj == null || getClass() != obj.getClass()) return false; 
//—— null 或不同类返回不等。
PuzzleMove that = (PuzzleMove) obj; return direction == that.direction; 
//—— 强转后比较枚举常量（使用 ==，对枚举是正确且高效的比较方式）。
@Override public int hashCode() { return direction.hashCode(); }
//返回方向的哈希，保持与 equals 一致，适合用作集合键或去重。
```

#### 实现 `State` 接口。
这是核心，将包含棋盘数据、启发式函数（错位、曼哈顿）的计算逻辑、`equals` 和 `hashCode` 方法  

修改 WattAi/src/stud/g01/problem/npuzzle/PuzzleBoard.java  

#### 实现 `Problem` 接口。
将包含问题是否可解（`solvable`）的逻辑、`applicable`（动作是否可用）以及`showSolution`（打印解法）。  

修改 WattAi/src/stud/g01/problem/npuzzle/NPuzzleProblem.java  

#### 实现 `Frontier` 接口。
这是一个高效的优先队列，用于 A* 算法。  

修改 WattAi/src/stud/g01/queue/PqFrontier.java  

#### 实现 `EngineFeeder`
负责从文本文件（`problems.txt`）中解析和创建 `NPuzzleProblem` 实例，并提供 A* 算法所需的 `Frontier` 和 `Predictor`（启发式函数）。  

修改WattAi/src/stud/g01/runner/PuzzleFeeder.java  

#### 运行测试
运行 `core.runner.SearchTester`，并设置程序参数（Program Arguments）： `resources/problems.txt NPUZZLE 1 stud.g01.runner.PuzzleFeeder`

+ `arg0`: `resources/problems.txt` (问题文件)
+ `arg1`: `NPUZZLE` (问题类型)
+ `arg2`: `1` (阶段 1)
+ `arg3`: `stud.g01.runner.PuzzleFeeder` (你的 Feeder 类)

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762495183757-9a98d421-0dfa-4b3b-a9e4-5aea964d9268.png)

#### 结果分析
![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762495526992-7d7f5416-48d3-420b-be93-844cf59b94e8.png)

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762495481693-9f3a7ce5-055d-469f-8dc8-7c5404c4e134.png)

实验数据清晰地证明了曼哈顿距离 (MANHATTAN) 是一个比错位将牌 (MISPLACED) 高效得多的启发函数。

它之所以“更智能”，是因为它提供的“估计值” h(n)更接近真实的最短距离 h*(n)。这使得 A* 算法能够更早地剪掉那些“看起来很有希望但实际上是死胡同”的路径，从而极大地减少了搜索的节点数量。

### 第二阶段
#### 修改`PuzzleBoard.java`
把**目标棋盘的位置**（`goalPositionsCache`）设置成了 `static`（静态）变量。  

更新 `WattAi/src/stud/g01/problem/npuzzle/PuzzleBoard.java`

需要让 `manhattan` 方法变得更“聪明”：在计算距离之前，它必须检查缓存里的数据是否是**当前这个目标**的。如果不是，就清空并重建缓存。  

#### 修改 `IdAStar.java`
实现 `stud.g01.solver.IdAStar.java`。  

#### 更新 `SearchTester.java`
更新 `core/runner/SearchTester.java` 文件  

在 `getHeuristicTypes` 方法中注释掉了 `MISPLACED` 启发函数。现在 A* 算法将只使用我们更高效的 `MANHATTAN` 启发函数来运行。  

#### 运行测试
运行 `core.runner.SearchTester`，并设置程序参数（Program Arguments）： 

`resources/problems3.txt NPUZZLE 3 stud.g01.runner.PuzzleFeeder`

#### 结果分析
![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762501214885-eb46a575-ae18-43a9-8ec4-13b71db38f9c.png)

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762501186042-8e846188-bfad-4670-964c-ec48cf087ce9.png)

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762501154301-cbbecea3-7ff0-44d2-9a50-172c912e58c8.png)

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762501128485-d348a515-12ce-49bf-b6de-eb3594366516.png)

**第二阶段成功。** 我们已经达到了 A* 算法的性能极限，这符合项目第二阶段的要求。

A* 算法为了保证最优解，必须把所有扩展过的节点（`explored` 集合）和所有待扩展的节点（`frontier` 队列）**全部保存在内存中**。

#### 修改`**PuzzleBoard.java**`
实现 `linearConflicts()` 的计算逻辑，并将其添加到 `predictor()`。  

#### 更新 `SearchTester.java`
修改 `core/runner/SearchTester.java`

#### 结果分析
![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762503044712-b4ece10b-127e-4094-971f-feeb1803303f.png)![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762503065109-71ca6472-16fb-479d-b03a-ef076a6a0d1a.png)

![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762503088229-153f9faa-db80-44d4-8f01-ccd701490e17.png)![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762503111195-78a4bb97-351c-454a-87af-ff4f47ff4869.png)

优化后，这个更“智能”的 h 函数极大地减少了搜索空间（将扩展节点从 1.23 亿减少到 970 万）。

### 第三阶段
能够在一分钟之内,解出以下实例。论述对第三阶段的问题实例进行求解的**模式数据库启发(Pattern Database Heuristics)算法**及其实现  

+ **IDA***：一种_内存效率极高_的搜索算法。
+ **模式数据库 (PDB)**：一种比曼哈顿距离更强大的_启发函数_。

**解决方案：实现 IDA* (Iterative Deepening A*)**

**工作原理：**

+ 它本质上是一个**深度优先搜索 (DFS)**。
+ DFS 的内存占用非常小（只保存当前路径），**这从根本上解决了 A* 的内存溢出问题**。
+ 它使用一个 f值“界限”（bound）。它会进行一次 DFS，剪掉（prune）任何 f值超过 `bound` 的分支。
+ 如果没找到解，它会把 `bound` 增加到“下一个最小的被剪掉的 f 值”，然后**重新开始**整个搜索。
+ 它会牺牲 CPU 时间（重复搜索）来换取巨大的内存优势。

**什么是模式数据库 (PDB)？**

+ `MANHATTAN` 是一个很好的启发函数，但还不够好。
+ PDB 是一种**更强**的启发函数。
+ **工作原理：** 你把 15-Puzzle 分成两组（例如，方块 1-7 和方块 8-15）。你预先计算并存储_只移动 1-7_（把 8-15 当作空白）到它们目标位置的**所有**可能步数。
+ **结果：**`h(n) = PDB(方块 1-7) + PDB(方块 8-15)`。这个 h 值比 `MANHATTAN` 精确得多，它能让 IDA* 跳过更多的错误分支，从而**极大缩短搜索时间**。

实现模式数据库（PDB）来替换 `MANHATTAN`，让 IDA* 运行得更快。

**添加**`AiProject/src/stud/g01/solver/PatternDatabase.java` 文件。  

**修改**`AiProject/src/stud/g01/problem/PuzzleBoard.java` 的内容。

**修改**`AiProject/src/core/runner/SearchTester.java` 的内容。

**修改**`AiProject/src/stud/g01/solver/IdAStar.java` 优化的 O(1) 版本。

#### 结果分析
![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762521923258-9f7efb45-41d4-409e-8a64-edd5336f2869.png)![](https://cdn.nlark.com/yuque/0/2025/png/44277119/1762522054484-77ffbf00-094d-4abb-b73a-33f941c10357.png)

+ PDB 构建时间： 29 秒（这是一个**一次性的预处理成本**）。
+ PDB 搜索时间： PDB 构建完成后，解决任何 15-Puzzle 问题的摊销时间（Amortized Time）都极低，0.03s以内

“1分钟之内”这个看似宽松的限制：这个限制是用来**覆盖那一次 PDB 构建成本的**。该程序（29秒构建 + 之后秒解所有问题）出色地完成了任务。

### 第四阶段
可视化阶段，使用SFML或者Unity3D等其他游戏开发引擎，实现解路径的可视化。不要求与搜索程序联动，只需要根据搜索程序给出的**多个问题的解路径文本文件**，进行可视化即可

### 项目总结
+ **阶段 1 (A* + Manhattan):** 我们在 1 秒内解决了所有 8-Puzzle。
+ **阶段 2 (A* 的极限):** 我们证明了 A* 在面对 15-Puzzle 难题时会**内存溢出 (OOM)**。
+ **阶段 3**
    - ** (IDA* 解决内存):** 我们实现了 IDA*，它**没有内存溢出**，并成功解决了 A* 无法处理的难题（尽管耗时 81.65 秒）。
    - **(强力启发函数解决时间):** 实现了 `MANHATTAN_PLUS_LINEAR_CONFLICTS`。这个更“智能”的 h 函数极大地减少了搜索空间（将扩展节点从 1.23 亿减少到 970 万），使得最后一个难题现在**完全在“1 分钟内”的要求之内**。
+ **阶段 4**

