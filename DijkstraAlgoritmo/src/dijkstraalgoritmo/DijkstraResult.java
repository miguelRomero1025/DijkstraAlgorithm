package dijkstraalgoritmo;

import dijkstraalgoritmo.model.Vertex;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Encapsula todos los resultados de una ejecución de Dijkstra:
 *  - Nodo fuente
 *  - Mapa de distancias mínimas desde la fuente a cada vértice
 *  - Camino óptimo hacia un destino concreto
 *  - Caminos secundarios (alternativas) hacia ese destino
 */
public class DijkstraResult {

    private final Vertex source;
    private final Map<Vertex, Integer> distances;
    private final LinkedList<Vertex>   optimalPath;
    private final List<PathResult>     secondaryPaths;

    public DijkstraResult(Vertex source,
                          Map<Vertex, Integer> distances,
                          LinkedList<Vertex>   optimalPath,
                          List<PathResult>     secondaryPaths) {
        this.source         = source;
        this.distances      = distances;
        this.optimalPath    = optimalPath;
        this.secondaryPaths = secondaryPaths;
    }

    public Vertex getSource()                       { return source; }
    public Map<Vertex, Integer> getDistances()      { return distances; }
    public LinkedList<Vertex>   getOptimalPath()    { return optimalPath; }
    public List<PathResult>     getSecondaryPaths() { return secondaryPaths; }

    /** Distancia mínima al destino (Integer.MAX_VALUE si no hay camino). */
    public int getOptimalCost() {
        if (optimalPath == null || optimalPath.isEmpty()) return Integer.MAX_VALUE;
        Vertex dest = optimalPath.getLast();
        return distances.getOrDefault(dest, Integer.MAX_VALUE);
    }

    public boolean hasPath() {
        return optimalPath != null && !optimalPath.isEmpty();
    }

    // ------------------------------------------------------------------ //

    /**
     * Representa un camino alternativo con su costo total.
     */
    public static class PathResult {
        private final LinkedList<Vertex> path;
        private final int cost;

        public PathResult(LinkedList<Vertex> path, int cost) {
            this.path = path;
            this.cost = cost;
        }

        public LinkedList<Vertex> getPath() { return path; }
        public int getCost()                { return cost; }

        @Override
        public String toString() {
            return path + " (costo: " + cost + ")";
        }
    }
}
