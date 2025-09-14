# TSP Solver & Visualizer (MVC + Swing + Concurrency)

A compact, teaching-oriented Java app to **solve and visualize the Travelling Salesman Problem (TSP)**. It ships with a Swing GUI, a clean **Model–View–Controller** design, and multiple exact & heuristic algorithms: **Brute Force**, **Nearest Neighbor (Greedy)**, **Held–Karp (Dynamic Programming)**, and **Branch & Bound** in both **sequential** and **concurrent (Fork/Join)** variants.&#x20;

> Built for Advanced Algorithms coursework. The (Catalan) report explains the architecture, algorithms, and concurrency findings in depth.&#x20;

---

## Features

* **Interactive GUI (Swing)**

  * Load or generate graphs via **adjacency matrices**.
  * Pick algorithm, start/stop runs, and see **progress, best cost, explored/p pruned nodes**, and the **current best tour** on a canvas.&#x20;
* **Algorithms you can compare**

  * **Brute Force** (exact, factorial time).
  * **Nearest Neighbor** (Greedy) + **multi-start parallel** variant.&#x20;
  * **Held–Karp** (DP with bitmasks), sequential and **parallelized** levels.&#x20;
  * **Branch & Bound**: sequential (matrix-reduction lower bound) and a **concurrent Fork/Join** version with atomic best-bound sharing.&#x20;
* **Event-driven MVC**

  * `NotificationService` decouples long computations from the UI; the controller routes events and keeps the EDT responsive.&#x20;

---


## Architecture (MVC + events)

```
View (Swing)
  └─ TSPView
       └─ GraphCanvas (drawing)
        ↑ UI events / progress updates
Controller
  └─ TSPController
        ↑ subscribes to notifications
        ↓ orchestrates runs, validates input
Model
  ├─ TSPModel (graph, distances, results)
  ├─ AlgorithmFactory (Factory Method)
  ├─ TSPAlgorithm (common contract)
  ├─ BruteForceTSP / GreedyTSP
  ├─ HeldKarpTSP (DP, bitmasks, parallel)
  ├─ BranchAndBound (sequential)
  └─ ConcurrentBranchAndBound (Fork/Join)
Infra
  ├─ NotificationService (pub/sub)
  └─ NotificationServiceImpl
```

The **Factory Method** centralizes algorithm instantiation via an `AlgorithmType` enum, and **publish/subscribe** notifications keep components loosely coupled.&#x20;

---

## Algorithms & complexity (at a glance)

* **Brute Force** — exact: **O(n!)** time, **O(n)** space (iterative permutation). Useful only for very small n.&#x20;
* **Nearest Neighbor (Greedy)** — heuristic: **O(n²)**, multi-start **parallel** variant improves quality by exploring all start cities concurrently.&#x20;
* **Held–Karp (DP)** — exact: **Θ(n²·2ⁿ)** time, **Θ(n·2ⁿ)** space; implemented with **bitmasks** and a **parallel level-wise** sweep for larger n.&#x20;
* **Branch & Bound** — exact: worst-case **O(n!)**, but practical performance depends on **lower bounds** and pruning; includes:

  * **Matrix-reduction** lower bound (sequential).
  * **Concurrent B\&B** with **Fork/Join**, precomputed per-city minimal edges as a fast bound, and **atomic** global best.&#x20;

> Empirical note: with moderate sizes (\~15–20 cities), the **sequential B\&B** outperformed the concurrent variant due to **atomic contention**, **task scheduling overhead**, and **too-fine granularity**—a reminder that parallelism must clear nontrivial overheads to win.&#x20;

---

## Concurrency model

* **Greedy multi-start** via an `ExecutorService` (embarrassingly parallel).&#x20;
* **Held–Karp parallel** using `ForkJoinPool` / `parallelStream()` over subset sizes.&#x20;
* **Concurrent B\&B**: recursive `ForkJoin` tasks, **atomic best-tour** updates, depth/branching thresholds to avoid oversplitting.&#x20;
* **Notifications** keep the EDT responsive; UI updates occur only on the Swing thread.&#x20;

---

## Key classes (what they do)

| Class/File                  | Role                                                                                     |
| --------------------------- | ---------------------------------------------------------------------------------------- |
| `TSPModel`                  | Central model: distances, algorithms, results, event publication.                        |
| `TSPView` + `GraphCanvas`   | Swing UI & drawing of graph and best tour.                                               |
| `TSPController`             | Bridges View↔Model; validates input; dispatches background runs; handles notifications.  |
| `AlgorithmFactory`          | **Factory Method** mapping `AlgorithmType → TSPAlgorithm`.                               |
| `TSPAlgorithm`              | Common interface for all solver implementations.                                         |
| `BruteForceTSP`             | Exact exhaustive search with small-n guardrails.                                         |
| `GreedyTSP`                 | Nearest-Neighbor; also **multi-start parallel**.                                         |
| `HeldKarpTSP`               | DP with bitmasks; sequential and parallel level sweeps.                                  |
| `BranchAndBound`            | Best-first with **matrix-reduction** lower bound.                                        |
| `ConcurrentBranchAndBound`  | Fork/Join B\&B with fast bound & atomic best sharing.                                    |
| `NotificationService(Impl)` | Pub/sub bus for progress, errors, and results.                                           |

---

## Further reading

* **Project report**: *Algorismes per al problema del viatjant de comerç: estudi de mètodes aproximats, concurrents i eficients* — architecture, algorithms, complexity analyses, and concurrency results.&#x20;

---


## Credits

* **Dylan Canning Garcia** and collaborators (see report). Thanks to the course staff for guidance and reviews.&#x20;

---

### Citation (if you use this in teaching/research)

> Canning Garcia, D., et al. *TSP Solver & Visualizer (MVC + Swing + Concurrency).* Project code and report, 2025.&#x20;

