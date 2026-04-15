package dijkstraalgoritmo;

import dijkstraalgoritmo.model.DijkstraResult;
import dijkstraalgoritmo.model.DijkstraResult.PathEntry;
import dijkstraalgoritmo.model.Edge;
import dijkstraalgoritmo.model.Graph;
import dijkstraalgoritmo.model.Vertex;

import java.util.*;

/**
 * Implementación del algoritmo de Dijkstra con soporte para:
 *  1. Camino óptimo (menor costo) entre dos vértices.
 *  2. Tabla de distancias mínimas desde el origen a todos los vértices.
 *  3. Caminos secundarios/alternativos (enfoque spur: excluye nodos intermedios).
 *
 * Uso:
 *   DijkstraAlgorithm algo = new DijkstraAlgorithm(graph);
 *   algo.execute(source);
 *   DijkstraResult result = algo.getResult(destination);
 */
public class DijkstraAlgorithm {

    private static final int DEFAULT_MAX_ALTERNATIVES = 3;

    private final List<Vertex> nodes;
    private final List<Edge>   edges;

    // Estado interno tras execute()
    private Set<Vertex>          settledNodes;
    private Set<Vertex>          unSettledNodes;
    private Map<Vertex, Vertex>  predecessors;
    private Map<Vertex, Integer> distance;
    private Vertex               executedSource;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DijkstraAlgorithm(Graph graph) {
        this.nodes = new ArrayList<>(graph.getVertexes());
        this.edges = new ArrayList<>(graph.getEdges());
    }

    // ── Ejecución principal ───────────────────────────────────────────────────

    /**
     * Ejecuta Dijkstra desde el vértice fuente.
     * Debe llamarse antes de getPath(), getDistanceTable() o getResult().
     */
    public void execute(Vertex source) {
        if (source == null)
            throw new IllegalArgumentException("El vértice origen no puede ser nulo.");
        if (!nodes.contains(source))
            throw new IllegalArgumentException("El vértice origen no pertenece al grafo.");

        settledNodes   = new HashSet<>();
        unSettledNodes = new HashSet<>();
        distance       = new HashMap<>();
        predecessors   = new HashMap<>();
        executedSource = source;

        distance.put(source, 0);
        unSettledNodes.add(source);

        while (!unSettledNodes.isEmpty()) {
            Vertex node = getMinimumFrom(unSettledNodes, distance);
            settledNodes.add(node);
            unSettledNodes.remove(node);
            findMinimalDistances(node);
        }
    }

    // ── Resultado completo ────────────────────────────────────────────────────

    /**
     * Retorna un DijkstraResult con camino óptimo, tabla de distancias
     * y caminos alternativos hacia el destino indicado.
     */
    public DijkstraResult getResult(Vertex destination) {
        return getResult(destination, DEFAULT_MAX_ALTERNATIVES);
    }

    public DijkstraResult getResult(Vertex destination, int maxAlternatives) {
        checkExecuted();

        LinkedList<Vertex> optimal = getPath(destination);
        int optimalCost = (optimal != null)
                ? distance.getOrDefault(destination, Integer.MAX_VALUE)
                : Integer.MAX_VALUE;

        List<PathEntry> alternatives =
                findAlternativePaths(executedSource, destination, optimal, maxAlternatives);

        return new DijkstraResult(
                executedSource, destination,
                optimal, optimalCost,
                new HashMap<>(distance),
                alternatives);
    }

    // ── API pública básica (compatible con código original) ───────────────────

    /**
     * Camino más corto desde el origen ejecutado hasta target.
     * @return lista ordenada de vértices, o null si no hay camino.
     */
    public LinkedList<Vertex> getPath(Vertex target) {
        checkExecuted();
        LinkedList<Vertex> path = new LinkedList<>();
        Vertex step = target;

        if (predecessors.get(step) == null && !step.equals(executedSource))
            return null;

        path.add(step);
        while (predecessors.get(step) != null) {
            step = predecessors.get(step);
            path.add(step);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Mapa completo vértice → distancia mínima desde el origen.
     * Vértices no alcanzables aparecen con Integer.MAX_VALUE.
     */
    public Map<Vertex, Integer> getDistanceTable() {
        checkExecuted();
        Map<Vertex, Integer> full = new HashMap<>(distance);
        for (Vertex v : nodes) full.putIfAbsent(v, Integer.MAX_VALUE);
        return Collections.unmodifiableMap(full);
    }

    /** Distancia mínima al vértice dado, o Integer.MAX_VALUE si no es alcanzable. */
    public int getShortestDistance(Vertex destination) {
        checkExecuted();
        return distance.getOrDefault(destination, Integer.MAX_VALUE);
    }

    // ── Caminos alternativos ──────────────────────────────────────────────────

    /**
     * Por cada nodo intermedio del camino óptimo lo excluye y recalcula,
     * acumulando caminos distintos ordenados por costo.
     */
    private List<PathEntry> findAlternativePaths(
            Vertex source, Vertex destination,
            LinkedList<Vertex> optimalPath, int maxAlternatives) {

        List<PathEntry> alternatives = new ArrayList<>();
        if (optimalPath == null || optimalPath.size() < 2) return alternatives;

        Set<List<Vertex>> seen = new HashSet<>();
        seen.add(optimalPath);

        for (int i = 1; i < optimalPath.size() - 1 && alternatives.size() < maxAlternatives; i++) {
            Vertex excluded = optimalPath.get(i);
            LinkedList<Vertex> alt = dijkstraExcluding(source, destination, Set.of(excluded));
            if (alt != null && !seen.contains(alt)) {
                alternatives.add(new PathEntry(alt, computePathCost(alt)));
                seen.add(alt);
            }
        }

        alternatives.sort(Comparator.comparingInt(PathEntry::getCost));
        return alternatives;
    }

    /**
     * Ejecuta Dijkstra sobre el mismo grafo excluyendo ciertos vértices intermedios.
     */
    private LinkedList<Vertex> dijkstraExcluding(
            Vertex source, Vertex destination, Set<Vertex> excluded) {

        Map<Vertex, Integer> dist      = new HashMap<>();
        Map<Vertex, Vertex>  pred      = new HashMap<>();
        Set<Vertex>          settled   = new HashSet<>();
        Set<Vertex>          unsettled = new HashSet<>();

        dist.put(source, 0);
        unsettled.add(source);

        while (!unsettled.isEmpty()) {
            Vertex node = getMinimumFrom(unsettled, dist);
            if (node.equals(destination)) break;
            settled.add(node);
            unsettled.remove(node);

            for (Edge edge : edges) {
                Vertex neighbor = neighborOf(edge, node);
                if (neighbor == null || settled.contains(neighbor)) continue;
                if (excluded.contains(neighbor) && !neighbor.equals(destination)) continue;

                int base = dist.getOrDefault(node, Integer.MAX_VALUE);
                if (base == Integer.MAX_VALUE) continue;
                int newDist = base + edge.getWeight();

                if (newDist < dist.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    dist.put(neighbor, newDist);
                    pred.put(neighbor, node);
                    unsettled.add(neighbor);
                }
            }
        }

        if (!dist.containsKey(destination)) return null;

        LinkedList<Vertex> path = new LinkedList<>();
        Vertex step = destination;
        if (pred.get(step) == null && !step.equals(source)) return null;
        path.add(step);
        while (pred.get(step) != null) {
            step = pred.get(step);
            path.add(step);
        }
        Collections.reverse(path);
        return path;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void findMinimalDistances(Vertex node) {
        for (Vertex target : getNeighbors(node)) {
            int current  = distance.getOrDefault(target, Integer.MAX_VALUE);
            int via      = distance.getOrDefault(node, Integer.MAX_VALUE);
            int edgeW    = getDistance(node, target);
            if (via != Integer.MAX_VALUE && current > via + edgeW) {
                distance.put(target, via + edgeW);
                predecessors.put(target, node);
                unSettledNodes.add(target);
            }
        }
    }

    private int getDistance(Vertex node, Vertex target) {
        for (Edge edge : edges) {
            if (edge.getSource().equals(node) && edge.getDestination().equals(target))
                return edge.getWeight();
            if (edge.isBidirectional()
                    && edge.getDestination().equals(node)
                    && edge.getSource().equals(target))
                return edge.getWeight();
        }
        throw new RuntimeException("No existe arista entre " + node + " y " + target);
    }

    private List<Vertex> getNeighbors(Vertex node) {
        List<Vertex> neighbors = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.getSource().equals(node) && !settledNodes.contains(edge.getDestination()))
                neighbors.add(edge.getDestination());
            else if (edge.isBidirectional()
                    && edge.getDestination().equals(node)
                    && !settledNodes.contains(edge.getSource()))
                neighbors.add(edge.getSource());
        }
        return neighbors;
    }

    /** Si la arista toca 'node', retorna el otro extremo; si no, null. */
    private Vertex neighborOf(Edge edge, Vertex node) {
        if (edge.getSource().equals(node)) return edge.getDestination();
        if (edge.isBidirectional() && edge.getDestination().equals(node)) return edge.getSource();
        return null;
    }

    private Vertex getMinimumFrom(Set<Vertex> vertexes, Map<Vertex, Integer> dist) {
        Vertex minimum = null;
        for (Vertex v : vertexes) {
            if (minimum == null
                    || dist.getOrDefault(v, Integer.MAX_VALUE)
                    < dist.getOrDefault(minimum, Integer.MAX_VALUE)) {
                minimum = v;
            }
        }
        return minimum;
    }

    /** Calcula el costo total de un camino. */
    public int computePathCost(List<Vertex> path) {
        if (path == null || path.size() < 2) return 0;
        int total = 0;
        for (int i = 0; i < path.size() - 1; i++)
            total += getDistance(path.get(i), path.get(i + 1));
        return total;
    }

    private void checkExecuted() {
        if (settledNodes == null)
            throw new IllegalStateException(
                    "Debe llamar a execute(source) antes de consultar resultados.");
    }
}
