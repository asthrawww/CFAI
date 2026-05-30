import heapq
import random
from dataclasses import dataclass
from typing import Dict, List, Tuple

# ======================================================
# EMERGENCY EVACUATION PATH PLANNER (FINAL CORRECT)
# ======================================================

# ------------------------------------------------------
# CO1: Problem Formulation (Graph + State Space)
# ------------------------------------------------------

class Graph:
    def __init__(self):
        self.graph: Dict[str, List[Tuple[str, int]]] = {}
        self.positions = {}
        self.risk = {}

    def add_room(self, room, pos):
        self.graph[room] = []
        self.positions[room] = pos

    def add_path(self, a, b, dist):
        self.graph[a].append((b, dist))
        self.graph[b].append((a, dist))

        r = random.uniform(0.1, 0.5)
        self.risk[(a, b)] = r
        self.risk[(b, a)] = r

    def remove_path(self, a, b):
        self.graph[a] = [x for x in self.graph[a] if x[0] != b]
        self.graph[b] = [x for x in self.graph[b] if x[0] != a]

    def display(self):
        print("\n📍 BUILDING LAYOUT")
        print("-" * 40)
        for room in self.graph:
            print(f"{room:<10} -> {self.graph[room]}")


# ------------------------------------------------------
# CO2: Search Algorithms
# ------------------------------------------------------

def heuristic(p1, p2):
    return abs(p1[0] - p2[0]) + abs(p1[1] - p2[1])


def bfs(graph, start, goal):
    queue = [(start, [start])]
    visited = set()

    while queue:
        node, path = queue.pop(0)
        if node == goal:
            return path
        if node not in visited:
            visited.add(node)
            for neigh, _ in graph.graph[node]:
                queue.append((neigh, path + [neigh]))
    return []


def dfs(graph, start, goal):
    stack = [(start, [start])]
    visited = set()

    while stack:
        node, path = stack.pop()
        if node == goal:
            return path
        if node not in visited:
            visited.add(node)
            for neigh, _ in graph.graph[node]:
                stack.append((neigh, path + [neigh]))
    return []


def greedy_bfs(graph, start, goal):
    pq = [(0, start, [start])]
    visited = set()

    while pq:
        _, node, path = heapq.heappop(pq)
        if node == goal:
            return path
        if node not in visited:
            visited.add(node)
            for neigh, _ in graph.graph[node]:
                h = heuristic(graph.positions[neigh], graph.positions[goal])
                heapq.heappush(pq, (h, neigh, path + [neigh]))
    return []


def astar(graph, start, goal):
    open_set = [(0, start)]
    g = {n: float('inf') for n in graph.graph}
    prev = {}

    g[start] = 0

    while open_set:
        _, current = heapq.heappop(open_set)

        if current == goal:
            break

        for neigh, w in graph.graph[current]:
            temp = g[current] + w
            if temp < g[neigh]:
                prev[neigh] = current
                g[neigh] = temp
                f = temp + heuristic(graph.positions[neigh], graph.positions[goal])
                heapq.heappush(open_set, (f, neigh))

    # reconstruct path
    path = []
    cur = goal

    if cur not in prev and cur != start:
        return []   # ✅ FIX: no path case handled

    while cur in prev:
        path.append(cur)
        cur = prev[cur]

    path.append(start)
    path.reverse()
    return path


# ------------------------------------------------------
# CO3: Constraint Satisfaction (SAFE PATH CHECK)
# ------------------------------------------------------

def is_safe(graph, path):
    if not path or len(path) == 1:
        return False

    for i in range(len(path) - 1):
        edge_risk = graph.risk.get((path[i], path[i+1]), 1)

        if edge_risk > 0.4:
            print(f"⚠️ Unsafe Edge: {path[i]} → {path[i+1]} (risk={round(edge_risk,2)})")
            return False

    return True


# ------------------------------------------------------
# CO4 + CO5: Cost, Risk, Utility
# ------------------------------------------------------

def compute_cost(graph, path):
    return sum(
        next(w for (n, w) in graph.graph[path[i]] if n == path[i+1])
        for i in range(len(path) - 1)
    )


def compute_risk(graph, path):
    return sum(
        graph.risk[(path[i], path[i+1])]
        for i in range(len(path) - 1)
    )


def expected_utility(cost, risk, prob_safe, risk_weight):
    return prob_safe * (-(cost + risk_weight * risk)) + (1 - prob_safe) * (-100)


# ------------------------------------------------------
# CO5: Hazard Simulation
# ------------------------------------------------------

def simulate_hazard(graph):
    print("\n🔥 HAZARD DETECTED!")

    edges = []
    for a in graph.graph:
        for b, _ in graph.graph[a]:
            if (b, a) not in edges:
                edges.append((a, b))

    blocked = random.choice(edges)
    graph.remove_path(blocked[0], blocked[1])

    print(f"🚧 Blocked Path: {blocked[0]} <--> {blocked[1]}")


# ------------------------------------------------------
# CO6: Hybrid Intelligent System
# ------------------------------------------------------

def main():

    print("\n🚨 EVACUATION SYSTEM STARTED 🚨")

    building = Graph()

    # Rooms
    building.add_room("Entrance", (0, 0))
    building.add_room("Hallway", (1, 0))
    building.add_room("RoomA", (2, 0))
    building.add_room("RoomB", (2, 1))
    building.add_room("Lab", (3, 1))
    building.add_room("Stairs", (1, 2))
    building.add_room("Exit", (0, 3))

    # Paths
    building.add_path("Entrance", "Hallway", 2)
    building.add_path("Hallway", "RoomA", 2)
    building.add_path("Hallway", "RoomB", 3)
    building.add_path("RoomA", "Lab", 4)
    building.add_path("RoomB", "Lab", 1)
    building.add_path("Hallway", "Stairs", 2)
    building.add_path("Stairs", "Exit", 3)
    building.add_path("RoomB", "Exit", 5)



    # -------- INPUT --------
    building.display()

    # -------- SHOW AVAILABLE LOCATIONS --------
    print("\n📌 Available Locations:")
    print("- Entrance")
    print("- Hallway")
    print("- RoomA")
    print("- RoomB")
    print("- Lab")
    print("- Stairs")
    print("- Exit")

    # -------- INPUT --------
    start = input("\nStart Location: ")
    goal = input("Goal Location: ")

    print("\nRisk Level: 1-Low  2-Medium  3-High")
    choice = input("Choose (1/2/3): ")

    risk_weight = 8 if choice == "1" else 5 if choice == "2" else 2
    prob_safe = 0.8

    # -------- RUN --------
    algorithms = {
        "BFS": bfs(building, start, goal),
        "DFS": dfs(building, start, goal),
        "Greedy": greedy_bfs(building, start, goal),
        "A*": astar(building, start, goal)
    }

    print("\n" + "="*45)
    print("        🚨 EVACUATION RESULTS 🚨")
    print("="*45)

    best_algo = None
    best_u = float('-inf')

    for name, path in algorithms.items():

        if not path:
            print(f"\n🔹 {name} RESULT\n   ❌ No Path Found")
            continue

        cost = compute_cost(building, path)
        risk = compute_risk(building, path)
        utility = expected_utility(cost, risk, prob_safe, risk_weight)

        print(f"\n🔹 {name} RESULT")
        print("   Path    :", " → ".join(path))
        print("   Cost    :", cost)
        print("   Risk    :", round(risk, 2))
        print("   Utility :", round(utility, 2))

        if utility > best_u:
            best_u = utility
            best_algo = name

    print("\n" + "="*45)
    print("🏆 BEST ALGORITHM:", best_algo)
    print("✔ Based on correct cost, risk and utility")
    print("="*45)

    # -------- HAZARD --------
    simulate_hazard(building)

    print("\n⚠️ Replanning after hazard...")
    new_path = astar(building, start, goal)

    # ✅ FINAL FIXED LOGIC
    if not new_path or new_path[-1] != goal:
        print("❌ No Path Available After Hazard!")
    elif is_safe(building, new_path):
        print("✅ Safe Path:", " → ".join(new_path))
    else:
        print("❌ No Safe Path Available!")


# ------------------------------------------------------
if __name__ == "__main__":
    main()

