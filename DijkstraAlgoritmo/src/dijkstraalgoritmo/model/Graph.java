package dijkstraalgoritmo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Representa el grafo completo.
 *
 * Cambios respecto al original:
 *  - Listas ya no son final en contenido: expone métodos add/remove
 *    para crear y eliminar vértices y aristas en runtime.
 *  - removeVertex elimina automáticamente todas las aristas incidentes
 *    (evita aristas huérfanas).
 *  - Validación de duplicados por id en add.
 *  - Vistas inmutables hacia afuera (Collections.unmodifiableList)
 *    para evitar modificaciones accidentales desde la vista.
 */
public class Graph {

    private final List<Vertex> vertexes = new ArrayList<>();
    private final List<Edge>   edges    = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public Graph() { }

    /** Constructor de copia compatible con el código original. */
    public Graph(List<Vertex> vertexes, List<Edge> edges) {
        this.vertexes.addAll(vertexes);
        this.edges.addAll(edges);
    }

    // ── Getters (vistas inmutables) ───────────────────────────────────────────

    public List<Vertex> getVertexes() {
        return Collections.unmodifiableList(vertexes);
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    // ── CRUD Vértices ─────────────────────────────────────────────────────────

    /**
     * Agrega un vértice al grafo.
     * @throws IllegalArgumentException si ya existe un vértice con el mismo id.
     */
    public void addVertex(Vertex v) {
        if (v == null)
            throw new IllegalArgumentException("El vértice no puede ser nulo.");
        if (containsVertex(v.getId()))
            throw new IllegalArgumentException("Ya existe un vértice con id: " + v.getId());
        vertexes.add(v);
    }

    /**
     * Elimina un vértice y todas sus aristas incidentes (entrantes y salientes).
     * @return true si fue eliminado, false si no existía.
     */
    public boolean removeVertex(String vertexId) {
        Optional<Vertex> found = findVertex(vertexId);
        if (found.isEmpty()) return false;

        Vertex v = found.get();
        // Eliminar todas las aristas que tocan este vértice
        edges.removeIf(e -> e.getSource().equals(v) || e.getDestination().equals(v));
        vertexes.remove(v);
        return true;
    }

    /** @return el vértice con ese id, o empty si no existe. */
    public Optional<Vertex> findVertex(String vertexId) {
        return vertexes.stream().filter(v -> v.getId().equals(vertexId)).findFirst();
    }

    public boolean containsVertex(String vertexId) {
        return findVertex(vertexId).isPresent();
    }

    // ── CRUD Aristas ──────────────────────────────────────────────────────────

    /**
     * Agrega una arista al grafo.
     * Valida que ambos vértices existan en el grafo y que no haya
     * ya una arista con el mismo id.
     */
    public void addEdge(Edge e) {
        if (e == null)
            throw new IllegalArgumentException("La arista no puede ser nula.");
        if (!containsVertex(e.getSource().getId()))
            throw new IllegalArgumentException("El vértice origen no pertenece al grafo: " + e.getSource().getId());
        if (!containsVertex(e.getDestination().getId()))
            throw new IllegalArgumentException("El vértice destino no pertenece al grafo: " + e.getDestination().getId());
        if (containsEdge(e.getId()))
            throw new IllegalArgumentException("Ya existe una arista con id: " + e.getId());
        edges.add(e);
    }

    /**
     * Elimina una arista por su id.
     * @return true si fue eliminada, false si no existía.
     */
    public boolean removeEdge(String edgeId) {
        return edges.removeIf(e -> e.getId().equals(edgeId));
    }

    /**
     * Actualiza el peso de una arista existente.
     * @throws IllegalArgumentException si la arista no existe.
     */
    public void updateEdgeWeight(String edgeId, int newWeight) {
        Edge e = findEdge(edgeId)
            .orElseThrow(() -> new IllegalArgumentException("No existe arista con id: " + edgeId));
        e.setWeight(newWeight);
    }

    /** @return la arista con ese id, o empty si no existe. */
    public Optional<Edge> findEdge(String edgeId) {
        return edges.stream().filter(e -> e.getId().equals(edgeId)).findFirst();
    }

    public boolean containsEdge(String edgeId) {
        return findEdge(edgeId).isPresent();
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /** Devuelve todas las aristas que parten desde el vértice dado. */
    public List<Edge> getEdgesFrom(Vertex v) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : edges) {
            if (e.getSource().equals(v)) result.add(e);
            else if (e.isBidirectional() && e.getDestination().equals(v)) result.add(e);
        }
        return result;
    }

    /** Elimina todos los vértices y aristas del grafo. */
    public void clear() {
        edges.clear();
        vertexes.clear();
    }

    public int vertexCount() { return vertexes.size(); }
    public int edgeCount()   { return edges.size(); }
}
