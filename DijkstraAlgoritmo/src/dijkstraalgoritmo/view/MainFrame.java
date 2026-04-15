package dijkstraalgoritmo.view;

import dijkstraalgoritmo.controller.GraphController;
import dijkstraalgoritmo.model.Vertex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Ventana principal de la aplicación.
 *
 * Layout:
 * ┌─────────────────────────────────────┐
 * │  Barra de herramientas              │
 * ├─────────────────────────────────────┤
 * │                                     │
 * │         GraphPanel (canvas)         │
 * │                                     │
 * ├─────────────────────────────────────┤
 * │  InfoPanel (tabla + caminos)        │
 * └─────────────────────────────────────┘
 */
public class MainFrame extends JFrame {

    // ------------------------------------------------------------------ //
    //  Colores
    // ------------------------------------------------------------------ //

    private static final Color TOOLBAR_BG   = new Color(22, 24, 38);
    private static final Color BTN_BG       = new Color(40, 45, 70);
    private static final Color BTN_HOVER    = new Color(60, 70, 120);
    private static final Color BTN_TEXT     = new Color(200, 210, 255);
    private static final Color BTN_ACTION   = new Color(50, 110, 200);
    private static final Color BTN_DANGER   = new Color(160,  50,  50);
    private static final Color SEPARATOR    = new Color(45,  50,  80);

    // ------------------------------------------------------------------ //
    //  Componentes y controlador
    // ------------------------------------------------------------------ //

    private final GraphController controller;
    private final GraphPanel      graphPanel;
    private final InfoPanel       infoPanel;

    // Combos de selección de nodos para Dijkstra
    private JComboBox<String> comboSource;
    private JComboBox<String> comboDest;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public MainFrame() {
        super("Visualizador de Dijkstra");
        controller = new GraphController();

        graphPanel = new GraphPanel(controller);
        infoPanel  = new InfoPanel();

        controller.setViews(graphPanel, infoPanel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setBackground(new Color(18, 20, 30));

        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(graphPanel) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(new Color(18, 20, 30));
        }}, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);

        // Registrar callback para actualizar combos cuando cambia el grafo
        controller.setOnGraphChanged(this::refreshCombos);

        pack();
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ------------------------------------------------------------------ //
    //  Barra de herramientas
    // ------------------------------------------------------------------ //

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBackground(TOOLBAR_BG);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, SEPARATOR),
            new EmptyBorder(2, 6, 2, 6)
        ));

        // -- Sección izquierda: acciones de grafo --
        toolbar.add(makeLabel("Grafo:"));
        toolbar.add(makeBtn("+ Nodo",       "Doble clic en el canvas para agregar un nodo", false, false));
        toolbar.add(makeBtn("🔄 Limpiar",   "Eliminar todos los nodos y conexiones", false, true, () -> {
            int ok = JOptionPane.showConfirmDialog(this,
                "¿Eliminar todos los nodos y conexiones?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                controller.resetGraph();
                refreshCombos();
            }
        }));

        toolbar.add(makeSeparator());

        // -- Sección central: ejecutar Dijkstra --
        toolbar.add(makeLabel("Origen:"));
        comboSource = makeCombo();
        toolbar.add(comboSource);

        toolbar.add(makeLabel("Destino:"));
        comboDest = makeCombo();
        toolbar.add(comboDest);

        JButton runBtn = makeBtn("▶  Ejecutar Dijkstra", "Calcular camino óptimo", true, false,
            () -> runDijkstra());
        toolbar.add(runBtn);

        JButton clearBtn = makeBtn("✕ Limpiar resultado", "Borrar el resultado resaltado", false, false,
            () -> {
                controller.clearResult();
                graphPanel.clearSelection();
            });
        toolbar.add(clearBtn);

        toolbar.add(makeSeparator());

        // -- Sección derecha: leyenda --
        toolbar.add(makeLegend("━", new Color(80, 220, 140), "Óptimo"));
        toolbar.add(makeLegend("━", new Color(255, 200,  80), "Alt. 1"));
        toolbar.add(makeLegend("━", new Color(255, 140,  80), "Alt. 2"));
        toolbar.add(makeLegend("━", new Color(200,  80, 255), "Alt. 3"));

        return toolbar;
    }

    // ------------------------------------------------------------------ //
    //  Acción Dijkstra
    // ------------------------------------------------------------------ //

    private void runDijkstra() {
        String srcName  = (String) comboSource.getSelectedItem();
        String destName = (String) comboDest.getSelectedItem();

        if (srcName == null || destName == null) {
            infoPanel.showError("Agrega al menos dos nodos antes de ejecutar.");
            return;
        }

        // Buscar vértices por nombre en el grafo
        List<Vertex> vertexes = controller.getGraph().getVertexes();
        Vertex src  = vertexes.stream().filter(v -> v.getName().equals(srcName)).findFirst().orElse(null);
        Vertex dest = vertexes.stream().filter(v -> v.getName().equals(destName)).findFirst().orElse(null);

        if (src == null || dest == null) {
            infoPanel.showError("Nodo no encontrado. Actualiza los combos.");
            return;
        }
        if (src.equals(dest)) {
            infoPanel.showError("El origen y el destino no pueden ser el mismo nodo.");
            return;
        }

        controller.runDijkstra(src.getId(), dest.getId());
    }

    // ------------------------------------------------------------------ //
    //  Actualizar combos al cambiar el grafo
    // ------------------------------------------------------------------ //

    /**
     * Sincroniza los JComboBox con los vértices actuales del grafo.
     * Llamado desde GraphController a través del controlador de propiedad.
     */
    public void refreshCombos() {
        String prevSrc  = (String) comboSource.getSelectedItem();
        String prevDest = (String) comboDest.getSelectedItem();

        comboSource.removeAllItems();
        comboDest.removeAllItems();

        for (Vertex v : controller.getGraph().getVertexes()) {
            comboSource.addItem(v.getName());
            comboDest.addItem(v.getName());
        }

        // Restaurar selección previa si aún existe
        if (prevSrc  != null) comboSource.setSelectedItem(prevSrc);
        if (prevDest != null) comboDest.setSelectedItem(prevDest);
    }

    // ------------------------------------------------------------------ //
    //  Builders de componentes de toolbar
    // ------------------------------------------------------------------ //

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(140, 150, 190));
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return l;
    }

    private JComboBox<String> makeCombo() {
        JComboBox<String> cb = new JComboBox<>();
        cb.setBackground(BTN_BG);
        cb.setForeground(BTN_TEXT);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setPreferredSize(new Dimension(90, 28));
        cb.setBorder(BorderFactory.createLineBorder(SEPARATOR, 1));
        return cb;
    }

    private JButton makeBtn(String text, String tooltip,
                             boolean isAction, boolean isDanger,
                             Runnable... action) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setForeground(BTN_TEXT);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 12, 5, 12));

        Color base = isDanger ? BTN_DANGER : isAction ? BTN_ACTION : BTN_BG;
        btn.setBackground(base);
        btn.setOpaque(true);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(base.brighter());
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(base);
            }
        });

        if (action.length > 0) btn.addActionListener(e -> action[0].run());
        return btn;
    }

    private JSeparator makeSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 26));
        sep.setForeground(SEPARATOR);
        return sep;
    }

    private JPanel makeLegend(String symbol, Color color, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        p.setBackground(TOOLBAR_BG);
        JLabel sym = new JLabel(symbol);
        sym.setForeground(color);
        sym.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(130, 140, 180));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        p.add(sym);
        p.add(lbl);
        return p;
    }

    // ------------------------------------------------------------------ //
    //  Entry point
    // ------------------------------------------------------------------ //

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame();
        });
    }
}
