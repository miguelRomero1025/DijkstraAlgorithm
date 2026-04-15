package dijkstraalgoritmo.view;

import dijkstraalgoritmo.DijkstraResult;
import dijkstraalgoritmo.controller.GraphController;
import dijkstraalgoritmo.model.Edge;
import dijkstraalgoritmo.model.Vertex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Canvas interactivo del grafo.
 *
 * Interacciones:
 *  - Doble clic en zona vacía  → crear nodo
 *  - Clic en nodo              → seleccionar
 *  - Arrastrar desde nodo      → conectar con otro nodo (suelta sobre el destino)
 *  - Clic derecho en nodo      → menú contextual (eliminar / renombrar)
 *  - Clic derecho en arista    → menú contextual (eliminar / editar peso)
 *  - Arrastrar nodo con Ctrl   → mover nodo
 */
public class GraphPanel extends JPanel {

    // ------------------------------------------------------------------ //
    //  Constantes visuales
    // ------------------------------------------------------------------ //

    public  static final int NODE_RADIUS    = 24;
    private static final int ARROW_SIZE     = 10;
    private static final int WEIGHT_PADDING = 6;

    // Paleta de colores
    private static final Color BG_COLOR          = new Color(18,  20,  30);
    private static final Color GRID_COLOR         = new Color(35,  38,  55);
    private static final Color NODE_FILL          = new Color(45,  50,  75);
    private static final Color NODE_BORDER        = new Color(100, 120, 200);
    private static final Color NODE_SELECTED_FILL = new Color(60,  80, 160);
    private static final Color NODE_SELECTED_BDR  = new Color(140, 180, 255);
    private static final Color NODE_TEXT          = new Color(220, 225, 255);
    private static final Color EDGE_COLOR         = new Color(80,  90, 130);
    private static final Color EDGE_WEIGHT_BG     = new Color(30,  33,  50, 210);
    private static final Color EDGE_WEIGHT_TEXT   = new Color(190, 200, 230);
    private static final Color DRAG_EDGE_COLOR    = new Color(120, 160, 255, 180);

    // Colores de resultados
    private static final Color OPTIMAL_EDGE_COLOR    = new Color(80, 220, 140);
    private static final Color OPTIMAL_NODE_FILL     = new Color(30, 100,  60);
    private static final Color OPTIMAL_NODE_BORDER   = new Color(80, 220, 140);
    private static final Color[] SECONDARY_COLORS    = {
        new Color(255, 200,  80),
        new Color(255, 140,  80),
        new Color(200,  80, 255)
    };

    // ------------------------------------------------------------------ //
    //  Estado interno
    // ------------------------------------------------------------------ //

    private final GraphController controller;

    /** Nodo que el usuario está arrastrando para crear una arista. */
    private Vertex  dragSourceVertex = null;
    private int     dragCurrentX     = 0;
    private int     dragCurrentY     = 0;
    private boolean isDraggingEdge   = false;

    /** Nodo que se está moviendo con Ctrl+arrastre. */
    private Vertex movingVertex = null;

    /** Último resultado de Dijkstra para colorear caminos. */
    private DijkstraResult result = null;

    /** Nodo seleccionado actualmente (resaltado). */
    private Vertex selectedVertex = null;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public GraphPanel(GraphController controller) {
        this.controller = controller;
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(800, 600));
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        initListeners();
    }

    // ------------------------------------------------------------------ //
    //  Listeners de ratón
    // ------------------------------------------------------------------ //

    private void initListeners() {
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftButton(e) && e.getClickCount() == 2) {
                    handleDoubleClick(e.getX(), e.getY());
                } else if (SwingUtilities.isLeftButton(e) && e.getClickCount() == 1) {
                    handleSingleClick(e.getX(), e.getY());
                } else if (SwingUtilities.isRightButton(e)) {
                    handleRightClick(e.getX(), e.getY(), e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftButton(e)) return;
                Vertex v = controller.getVertexAt(e.getX(), e.getY());
                if (v == null) return;

                if (e.isControlDown()) {
                    // Ctrl + arrastre → mover nodo
                    movingVertex = v;
                } else {
                    // Arrastre normal → conectar
                    dragSourceVertex = v;
                    dragCurrentX     = e.getX();
                    dragCurrentY     = e.getY();
                    isDraggingEdge   = false;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftButton(e)) {
                    cleanup();
                    return;
                }
                if (isDraggingEdge && dragSourceVertex != null) {
                    Vertex target = controller.getVertexAt(e.getX(), e.getY());
                    if (target != null && !target.equals(dragSourceVertex)) {
                        askAndCreateEdge(dragSourceVertex, target);
                    }
                }
                cleanup();
            }

            private void cleanup() {
                dragSourceVertex = null;
                isDraggingEdge   = false;
                movingVertex     = null;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (movingVertex != null) {
                    // Mover nodo
                    controller.getPositions().put(
                        movingVertex.getId(), new int[]{e.getX(), e.getY()}
                    );
                    repaint();
                    return;
                }
                if (dragSourceVertex != null) {
                    isDraggingEdge = true;
                    dragCurrentX   = e.getX();
                    dragCurrentY   = e.getY();
                    repaint();
                }
            }
        });
    }

    // ------------------------------------------------------------------ //
    //  Handlers de eventos
    // ------------------------------------------------------------------ //

    private void handleDoubleClick(int x, int y) {
        Vertex existing = controller.getVertexAt(x, y);
        if (existing != null) return; // doble clic sobre nodo existente → ignorar
        controller.addVertex(x, y);
    }

    private void handleSingleClick(int x, int y) {
        Vertex v = controller.getVertexAt(x, y);
        selectedVertex = v;
        controller.setSelectedVertex(v);
        repaint();
    }

    private void handleRightClick(int x, int y, MouseEvent e) {
        Vertex v = controller.getVertexAt(x, y);
        if (v != null) {
            showVertexContextMenu(v, e);
            return;
        }
        Edge edge = controller.getEdgeNear(x, y);
        if (edge != null) {
            showEdgeContextMenu(edge, e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Menús contextuales
    // ------------------------------------------------------------------ //

    private void showVertexContextMenu(Vertex v, MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        stylePopup(menu);

        JMenuItem rename = new JMenuItem("✏  Renombrar nodo");
        rename.addActionListener(ev -> {
            String newName = JOptionPane.showInputDialog(
                this, "Nuevo nombre:", v.getName());
            if (newName != null && !newName.isBlank())
                controller.renameVertex(v.getId(), newName.trim());
        });

        JMenuItem delete = new JMenuItem("🗑  Eliminar nodo");
        delete.addActionListener(ev -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el nodo \"" + v.getName() + "\" y todas sus conexiones?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION)
                controller.removeVertex(v.getId());
        });

        menu.add(rename);
        menu.addSeparator();
        menu.add(delete);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showEdgeContextMenu(Edge edge, MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        stylePopup(menu);

        JMenuItem editWeight = new JMenuItem("⚖  Editar costo");
        editWeight.addActionListener(ev -> {
            String input = JOptionPane.showInputDialog(
                this, "Nuevo costo (≥ 0):", edge.getWeight());
            if (input == null) return;
            try {
                int w = Integer.parseInt(input.trim());
                controller.updateEdgeWeight(edge.getId(), w);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Ingresa un número entero válido.");
            }
        });

        JMenuItem deleteEdge = new JMenuItem("🗑  Eliminar conexión");
        deleteEdge.addActionListener(ev -> controller.removeEdge(edge.getId()));

        menu.add(editWeight);
        menu.addSeparator();
        menu.add(deleteEdge);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void askAndCreateEdge(Vertex src, Vertex dest) {
        String input = JOptionPane.showInputDialog(
            this,
            "Costo de la conexión " + src.getName() + " → " + dest.getName() + ":",
            "1"
        );
        if (input == null) return;
        try {
            int weight = Integer.parseInt(input.trim());
            Edge created = controller.addEdge(src.getId(), dest.getId(), weight);
            if (created == null) {
                JOptionPane.showMessageDialog(this,
                    "Ya existe una conexión de " + src.getName() + " a " + dest.getName() + ".");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingresa un número entero válido.");
        }
    }

    // ------------------------------------------------------------------ //
    //  Pintura del canvas
    // ------------------------------------------------------------------ //

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        drawEdges(g2);
        drawDragLine(g2);
        drawVertexes(g2);
    }

    // -- Grid de fondo --

    private void drawGrid(Graphics2D g2) {
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(0.5f));
        int step = 40;
        for (int x = 0; x < getWidth(); x += step)
            g2.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += step)
            g2.drawLine(0, y, getWidth(), y);
    }

    // -- Aristas --

    private void drawEdges(Graphics2D g2) {
        Map<String, int[]> pos = controller.getPositions();

        for (Edge edge : controller.getGraph().getEdges()) {
            int[] sp = pos.get(edge.getSource().getId());
            int[] dp = pos.get(edge.getDestination().getId());
            if (sp == null || dp == null) continue;

            Color edgeColor = getEdgeColor(edge);
            float stroke    = isOptimalEdge(edge) ? 3.0f : isSecondaryEdge(edge) ? 2.2f : 1.5f;

            drawArrow(g2, sp[0], sp[1], dp[0], dp[1], edgeColor, stroke);
            drawWeightLabel(g2, sp[0], sp[1], dp[0], dp[1], edge.getWeight());
        }
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2,
                            Color color, float strokeWidth) {
        double angle  = Math.atan2(y2 - y1, x2 - x1);
        // Ajustar puntos para que la flecha no solape el círculo del nodo
        int ex = (int)(x2 - NODE_RADIUS * Math.cos(angle));
        int ey = (int)(y2 - NODE_RADIUS * Math.sin(angle));
        int sx = (int)(x1 + NODE_RADIUS * Math.cos(angle));
        int sy = (int)(y1 + NODE_RADIUS * Math.sin(angle));

        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(sx, sy, ex, ey);

        // Punta de flecha
        double ax1 = ex - ARROW_SIZE * Math.cos(angle - Math.PI / 7);
        double ay1 = ey - ARROW_SIZE * Math.sin(angle - Math.PI / 7);
        double ax2 = ex - ARROW_SIZE * Math.cos(angle + Math.PI / 7);
        double ay2 = ey - ARROW_SIZE * Math.sin(angle + Math.PI / 7);

        GeneralPath arrow = new GeneralPath();
        arrow.moveTo(ex, ey);
        arrow.lineTo(ax1, ay1);
        arrow.lineTo(ax2, ay2);
        arrow.closePath();
        g2.fill(arrow);
    }

    private void drawWeightLabel(Graphics2D g2, int x1, int y1, int x2, int y2, int weight) {
        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2;

        String text  = String.valueOf(weight);
        Font   font  = new Font("Monospaced", Font.BOLD, 12);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int th = fm.getAscent();

        // Fondo de la etiqueta
        RoundRectangle2D bg = new RoundRectangle2D.Float(
            mx - tw / 2f - WEIGHT_PADDING,
            my - th / 2f - WEIGHT_PADDING / 2f,
            tw + WEIGHT_PADDING * 2,
            th + WEIGHT_PADDING,
            6, 6
        );
        g2.setColor(EDGE_WEIGHT_BG);
        g2.fill(bg);

        g2.setColor(EDGE_WEIGHT_TEXT);
        g2.drawString(text, mx - tw / 2, my + th / 2 - 1);
    }

    // -- Línea de arrastre --

    private void drawDragLine(Graphics2D g2) {
        if (!isDraggingEdge || dragSourceVertex == null) return;
        int[] sp = controller.getPositions().get(dragSourceVertex.getId());
        if (sp == null) return;

        g2.setColor(DRAG_EDGE_COLOR);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                                     BasicStroke.JOIN_ROUND, 0, new float[]{8, 6}, 0));
        g2.drawLine(sp[0], sp[1], dragCurrentX, dragCurrentY);
    }

    // -- Vértices --

    private void drawVertexes(Graphics2D g2) {
        Map<String, int[]> pos = controller.getPositions();

        for (Vertex v : controller.getGraph().getVertexes()) {
            int[] p = pos.get(v.getId());
            if (p == null) continue;

            boolean isSelected = v.equals(selectedVertex);
            boolean isOptimal  = isOnOptimalPath(v);
            boolean isSecondary = isOnSecondaryPath(v);

            Color fill   = isOptimal  ? OPTIMAL_NODE_FILL
                         : isSelected ? NODE_SELECTED_FILL
                         : NODE_FILL;
            Color border = isOptimal  ? OPTIMAL_NODE_BORDER
                         : isSelected ? NODE_SELECTED_BDR
                         : NODE_BORDER;

            // Sombra
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval(p[0] - NODE_RADIUS + 3, p[1] - NODE_RADIUS + 3,
                        NODE_RADIUS * 2, NODE_RADIUS * 2);

            // Relleno
            g2.setColor(fill);
            g2.fillOval(p[0] - NODE_RADIUS, p[1] - NODE_RADIUS,
                        NODE_RADIUS * 2, NODE_RADIUS * 2);

            // Borde (más grueso si seleccionado u óptimo)
            float bw = (isSelected || isOptimal) ? 2.5f : 1.5f;
            g2.setColor(border);
            g2.setStroke(new BasicStroke(bw));
            g2.drawOval(p[0] - NODE_RADIUS, p[1] - NODE_RADIUS,
                        NODE_RADIUS * 2, NODE_RADIUS * 2);

            // Texto del nombre
            Font   font = new Font("SansSerif", Font.BOLD, 13);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            String name = v.getName();
            int tx = p[0] - fm.stringWidth(name) / 2;
            int ty = p[1] + fm.getAscent() / 2 - 2;
            g2.setColor(NODE_TEXT);
            g2.drawString(name, tx, ty);
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers de resultado
    // ------------------------------------------------------------------ //

    private boolean isOnOptimalPath(Vertex v) {
        if (result == null || result.getOptimalPath() == null) return false;
        return result.getOptimalPath().contains(v);
    }

    private boolean isOnSecondaryPath(Vertex v) {
        if (result == null) return false;
        for (DijkstraResult.PathResult pr : result.getSecondaryPaths())
            if (pr.getPath().contains(v)) return true;
        return false;
    }

    private boolean isOptimalEdge(Edge edge) {
        if (result == null || result.getOptimalPath() == null) return false;
        LinkedList<Vertex> path = result.getOptimalPath();
        return isEdgeOnPath(edge, path);
    }

    private boolean isSecondaryEdge(Edge edge) {
        if (result == null) return false;
        for (DijkstraResult.PathResult pr : result.getSecondaryPaths())
            if (isEdgeOnPath(edge, pr.getPath())) return true;
        return false;
    }

    private boolean isEdgeOnPath(Edge edge, LinkedList<Vertex> path) {
        List<Vertex> list = new java.util.ArrayList<>(path);
        for (int i = 0; i < list.size() - 1; i++) {
            if (edge.getSource().equals(list.get(i))
                    && edge.getDestination().equals(list.get(i + 1)))
                return true;
        }
        return false;
    }

    private Color getEdgeColor(Edge edge) {
        if (isOptimalEdge(edge)) return OPTIMAL_EDGE_COLOR;
        if (result != null) {
            List<DijkstraResult.PathResult> secondary = result.getSecondaryPaths();
            for (int i = 0; i < secondary.size(); i++) {
                if (isEdgeOnPath(edge, secondary.get(i).getPath()))
                    return SECONDARY_COLORS[i % SECONDARY_COLORS.length];
            }
        }
        return EDGE_COLOR;
    }

    // ------------------------------------------------------------------ //
    //  API pública
    // ------------------------------------------------------------------ //

    public void setResult(DijkstraResult result) {
        this.result = result;
    }

    public void clearSelection() {
        selectedVertex = null;
        controller.setSelectedVertex(null);
        repaint();
    }

    // ------------------------------------------------------------------ //
    //  Utilidades
    // ------------------------------------------------------------------ //

    private void stylePopup(JPopupMenu menu) {
        menu.setBackground(new Color(30, 33, 50));
        menu.setBorder(BorderFactory.createLineBorder(new Color(70, 80, 130), 1));
    }
}
