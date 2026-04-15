package dijkstraalgoritmo.controller;

import dijkstraalgoritmo.DijkstraAlgorithm;
import dijkstraalgoritmo.DijkstraResult;
import dijkstraalgoritmo.model.Edge;
import dijkstraalgoritmo.model.Graph;
import dijkstraalgoritmo.model.Vertex;
import dijkstraalgoritmo.view.GraphPanel;
import dijkstraalgoritmo.view.InfoPanel;
import dijkstraalgoritmo.view.MainFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controlador principal de la aplicación.
 *
 * Responsabilidades:
 *  - Gestionar el estado del grafo (agregar/eliminar nodos y aristas).
 *  - Responder a las interacciones del canvas (doble clic, arrastre).
 *  - Ejecutar el algoritmo de Dijkstra y distribuir los resultados a la vista.
 *  - Mantener las posiciones visuales de los nodos (x, y en el canvas).
 */
public class GraphController {

    // ------------------------------------------------------------------ //
    //  Estado del grafo y posiciones visuales
    // ------------------------------------------------------------------ //

    private final Graph graph;

    /** Posiciones visuales de cada nodo en el canvas (id -> punto). */
    private final Map<String, int[]> positions = new HashMap<>();

    /** Último resultado calculado (puede ser null si aún no se ejecutó). */
    private DijkstraResult lastResult;

    /** Nodo actualmente seleccionado (para operaciones de un solo nodo). */
    private Vertex selectedVertex;

    /** Número máximo de caminos secundarios a buscar. */
    private static final int MAX_SECONDARY_PATHS = 3;

    // ------------------------------------------------------------------ //
    //  Contadores para IDs únicos
    // ------------------------------------------------------------------ //

    private final AtomicInteger vertexCounter = new AtomicInteger(0);
    private final AtomicInteger edgeCounter   = new AtomicInteger(0);

    // ------------------------------------------------------------------ //
    //  Referencias a la vista
    // ------------------------------------------------------------------ //

    private GraphPanel graphPanel;
    private InfoPanel  infoPanel;

    /** Callback que se invoca cada vez que cambia la lista de vértices (para actualizar combos). */
    private Runnable onGraphChanged;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public GraphController() {
        this.graph = new Graph();
    }

    public void setViews(GraphPanel graphPanel, InfoPanel infoPanel) {
        this.graphPanel = graphPanel;
        this.infoPanel  = infoPanel;
    }

    /** Registra callback que se ejecuta al agregar/eliminar vértices (para actualizar combos). */
    public void setOnGraphChanged(Runnable callback) {
        this.onGraphChanged = callback;
    }

    private void notifyGraphChanged() {
        if (onGraphChanged != null) onGraphChanged.run();
    }

    // ------------------------------------------------------------------ //
    //  Operaciones sobre vértices
    // ------------------------------------------------------------------ //

    /**
     * Crea un nuevo vértice en la posición indicada del canvas.
     * El nombre se genera automáticamente (A, B, C, ... Z, A1, B1, ...).
     */
    public Vertex addVertex(int x, int y) {
        String id   = "V" + vertexCounter.incrementAndGet();
        String name = generateVertexName();
        Vertex v    = new Vertex(id, name);
        graph.addVertex(v);
        positions.put(id, new int[]{x, y});
        clearResult();
        notifyGraphChanged();
        refreshView();
        return v;
    }

    /**
     * Elimina el vértice con el id dado, junto con todas sus aristas.
     */
    public boolean removeVertex(String vertexId) {
        if (!graph.containsVertex(vertexId)) return false;
        positions.remove(vertexId);
        boolean removed = graph.removeVertex(vertexId);
        if (selectedVertex != null && selectedVertex.getId().equals(vertexId)) {
            selectedVertex = null;
        }
        clearResult();
        notifyGraphChanged();
        refreshView();
        return removed;
    }

    /**
     * Renombra un vértice existente.
     */
    public void renameVertex(String vertexId, String newName) {
        graph.findVertexById(vertexId).ifPresent(v -> v.setName(newName));
        refreshView();
    }

    // ------------------------------------------------------------------ //
    //  Operaciones sobre aristas
    // ------------------------------------------------------------------ //

    /**
     * Crea una arista dirigida entre dos vértices con el peso dado.
     * @return la arista creada, o null si ya existe una arista entre esos vértices.
     */
    public Edge addEdge(String sourceId, String destinationId, int weight) {
        Optional<Vertex> src  = graph.findVertexById(sourceId);
        Optional<Vertex> dest = graph.findVertexById(destinationId);

        if (src.isEmpty() || dest.isEmpty()) return null;
        if (sourceId.equals(destinationId))  return null;

        // Evitar arista duplicada en la misma dirección
        if (graph.findEdge(src.get(), dest.get()).isPresent()) return null;

        String id   = "E" + edgeCounter.incrementAndGet();
        Edge   edge = new Edge(id, src.get(), dest.get(), weight);
        graph.addEdge(edge);
        clearResult();
        refreshView();
        return edge;
    }

    /**
     * Elimina una arista por su id.
     */
    public boolean removeEdge(String edgeId) {
        boolean removed = graph.removeEdge(edgeId);
        if (removed) {
            clearResult();
            refreshView();
        }
        return removed;
    }

    /**
     * Actualiza el peso de una arista existente.
     */
    public void updateEdgeWeight(String edgeId, int newWeight) {
        graph.updateEdgeWeight(edgeId, newWeight);
        clearResult();
        refreshView();
    }

    // ------------------------------------------------------------------ //
    //  Algoritmo de Dijkstra
    // ------------------------------------------------------------------ //

    /**
     * Ejecuta Dijkstra desde {@code sourceId} hacia {@code destinationId}
     * y actualiza la vista con los resultados.
     *
     * @return el resultado, o null si los vértices no existen o el grafo está vacío.
     */
    public DijkstraResult runDijkstra(String sourceId, String destinationId) {
        Optional<Vertex> src  = graph.findVertexById(sourceId);
        Optional<Vertex> dest = graph.findVertexById(destinationId);

        if (src.isEmpty() || dest.isEmpty()) {
            infoPanel.showError("Selecciona un nodo origen y un nodo destino válidos.");
            return null;
        }

        try {
            DijkstraAlgorithm algo = new DijkstraAlgorithm(graph);
            lastResult = algo.execute(src.get(), dest.get(), MAX_SECONDARY_PATHS);

            graphPanel.setResult(lastResult);
            infoPanel.showResult(lastResult);
            graphPanel.repaint();
            return lastResult;

        } catch (Exception e) {
            infoPanel.showError("Error al ejecutar Dijkstra: " + e.getMessage());
            return null;
        }
    }

    /**
     * Limpia el resultado actual y restaura el canvas al estado neutro.
     */
    public void clearResult() {
        lastResult = null;
        if (graphPanel != null) {
            graphPanel.setResult(null);
            graphPanel.repaint();
        }
        if (infoPanel != null) {
            infoPanel.clear();
        }
    }

    /**
     * Reinicia el grafo completo.
     */
    public void resetGraph() {
        graph.clear();
        positions.clear();
        selectedVertex = null;
        vertexCounter.set(0);
        edgeCounter.set(0);
        clearResult();
        refreshView();
    }

    // ------------------------------------------------------------------ //
    //  Selección de nodos
    // ------------------------------------------------------------------ //

    public void setSelectedVertex(Vertex v) {
        this.selectedVertex = v;
    }

    public Vertex getSelectedVertex() {
        return selectedVertex;
    }

    // ------------------------------------------------------------------ //
    //  Consultas de estado
    // ------------------------------------------------------------------ //

    public Graph  getGraph()      { return graph; }
    public Map<String, int[]> getPositions() { return positions; }
    public DijkstraResult getLastResult()    { return lastResult; }

    /**
     * Devuelve el vértice cuyo círculo visual contiene el punto (x, y),
     * o null si no hay ninguno.
     */
    public Vertex getVertexAt(int x, int y) {
        final int RADIUS = GraphPanel.NODE_RADIUS;
        for (Vertex v : graph.getVertexes()) {
            int[] pos = positions.get(v.getId());
            if (pos == null) continue;
            int dx = pos[0] - x;
            int dy = pos[1] - y;
            if (dx * dx + dy * dy <= RADIUS * RADIUS) return v;
        }
        return null;
    }

    /**
     * Devuelve la arista cuya etiqueta de peso está cerca del punto (x, y),
     * o null si ninguna está suficientemente cerca.
     */
    public Edge getEdgeNear(int x, int y) {
        final int TOLERANCE = 12;
        for (Edge e : graph.getEdges()) {
            int[] sp = positions.get(e.getSource().getId());
            int[] dp = positions.get(e.getDestination().getId());
            if (sp == null || dp == null) continue;

            // Punto medio de la arista
            int mx = (sp[0] + dp[0]) / 2;
            int my = (sp[1] + dp[1]) / 2;

            if (Math.abs(mx - x) <= TOLERANCE && Math.abs(my - y) <= TOLERANCE) return e;
        }
        return null;
    }

    // ------------------------------------------------------------------ //
    //  Utilidades privadas
    // ------------------------------------------------------------------ //

    private void refreshView() {
        if (graphPanel != null) graphPanel.repaint();
    }

    /** Genera nombres de vértice: A, B, ..., Z, A1, B1, ... */
    private String generateVertexName() {
        int n     = vertexCounter.get() - 1; // ya fue incrementado
        int letra = n % 26;
        int ciclo = n / 26;
        String name = String.valueOf((char) ('A' + letra));
        if (ciclo > 0) name += ciclo;
        return name;
    }
}
