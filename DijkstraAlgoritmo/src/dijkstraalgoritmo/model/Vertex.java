package dijkstraalgoritmo.model;

/**
 * Representa un nodo/vértice del grafo.
 * El id es inmutable; name y posición (x, y) son mutables para
 * permitir renombrar y arrastrar nodos en el visualizador.
 */
public class Vertex {

    private final String id;
    private String name;

    /** Coordenadas en el canvas del visualizador. */
    private int x;
    private int y;

    // ── Constructores ─────────────────────────────────────────────────────────

    public Vertex(String id, String name, int x, int y) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("El id del vértice no puede ser nulo o vacío.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("El nombre del vértice no puede ser nulo o vacío.");
        this.id   = id;
        this.name = name;
        this.x    = x;
        this.y    = y;
    }

    /** Constructor sin posición (útil para pruebas o uso sin visualizador). */
    public Vertex(String id, String name) {
        this(id, name, 0, 0);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()   { return id; }
    public String getName() { return name; }
    public int    getX()    { return x; }
    public int    getY()    { return y; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("El nombre no puede ser vacío.");
        this.name = name;
    }

    public void setX(int x)              { this.x = x; }
    public void setY(int y)              { this.y = y; }
    public void setPosition(int x, int y){ this.x = x; this.y = y; }

    // ── equals / hashCode basados únicamente en id ────────────────────────────

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vertex)) return false;
        return id.equals(((Vertex) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
