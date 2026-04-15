package dijkstraalgoritmo.model;

/**
 * Representa una arista (conexión) entre dos vértices.
 *
 * Cambios respecto al original:
 *  - weight ya NO es final → permite actualizar el costo sin recrear la arista.
 *  - Campo bidirectional: si es true, el grafo la tratará como dos aristas
 *    (source→destination y destination→source).
 *  - Validaciones en constructor.
 */
public class Edge {

    private final String id;
    private final Vertex source;
    private final Vertex destination;
    private int weight;                 // mutable para poder actualizar costo
    private final boolean bidirectional;

    // ── Constructores ─────────────────────────────────────────────────────────

    public Edge(String id, Vertex source, Vertex destination, int weight, boolean bidirectional) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("El id de la arista no puede ser nulo o vacío.");
        if (source == null || destination == null)
            throw new IllegalArgumentException("La arista debe tener vértice origen y destino.");
        if (source.equals(destination))
            throw new IllegalArgumentException("Una arista no puede conectar un vértice consigo mismo.");
        if (weight < 0)
            throw new IllegalArgumentException("El peso de la arista no puede ser negativo.");

        this.id            = id;
        this.source        = source;
        this.destination   = destination;
        this.weight        = weight;
        this.bidirectional = bidirectional;
    }

    /** Crea una arista dirigida (source → destination). */
    public Edge(String id, Vertex source, Vertex destination, int weight) {
        this(id, source, destination, weight, false);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String  getId()            { return id; }
    public Vertex  getSource()        { return source; }
    public Vertex  getDestination()   { return destination; }
    public int     getWeight()        { return weight; }
    public boolean isBidirectional()  { return bidirectional; }

    // ── Setter de peso (caso de uso: actualizar costo de conexión) ─────────────

    public void setWeight(int weight) {
        if (weight < 0)
            throw new IllegalArgumentException("El peso no puede ser negativo.");
        this.weight = weight;
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /**
     * Indica si esta arista conecta los dos vértices dados
     * (en cualquier dirección si es bidireccional).
     */
    public boolean connects(Vertex a, Vertex b) {
        if (source.equals(a) && destination.equals(b)) return true;
        if (bidirectional && source.equals(b) && destination.equals(a)) return true;
        return false;
    }

    @Override
    public String toString() {
        String arrow = bidirectional ? " <-> " : " -> ";
        return source.getName() + arrow + destination.getName() + " (" + weight + ")";
    }
}
