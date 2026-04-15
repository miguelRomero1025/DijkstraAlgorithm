package dijkstraalgoritmo.view;

import dijkstraalgoritmo.DijkstraResult;
import dijkstraalgoritmo.model.Vertex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel inferior que muestra, tras ejecutar Dijkstra:
 *  - Tabla de distancias mínimas desde la fuente a todos los nodos.
 *  - Camino óptimo con su costo total.
 *  - Caminos secundarios (alternativos) con sus costos.
 */
public class InfoPanel extends JPanel {

    // ------------------------------------------------------------------ //
    //  Colores
    // ------------------------------------------------------------------ //

    private static final Color BG           = new Color(22, 24, 36);
    private static final Color PANEL_BG     = new Color(28, 31, 46);
    private static final Color HEADER_BG    = new Color(35, 40, 65);
    private static final Color TEXT_PRIMARY = new Color(210, 220, 255);
    private static final Color TEXT_MUTED   = new Color(110, 120, 160);
    private static final Color OPTIMAL_CLR  = new Color(80,  220, 140);
    private static final Color[] SEC_COLORS = {
        new Color(255, 200,  80),
        new Color(255, 140,  80),
        new Color(200,  80, 255)
    };
    private static final Color ERROR_CLR    = new Color(255, 100,  80);
    private static final Color TABLE_ROW_A  = new Color(28, 31, 46);
    private static final Color TABLE_ROW_B  = new Color(33, 37, 55);
    private static final Color TABLE_SELECT = new Color(50, 70, 130);

    // ------------------------------------------------------------------ //
    //  Componentes
    // ------------------------------------------------------------------ //

    private final JLabel          statusLabel;
    private final JTable          distanceTable;
    private final DefaultTableModel tableModel;
    private final JPanel          pathsPanel;
    private final JScrollPane     tableScroll;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public InfoPanel() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        setPreferredSize(new Dimension(0, 200));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 55, 85)));

        // -- Barra de estado superior --
        statusLabel = new JLabel("  Doble clic para agregar nodos · Arrastra entre nodos para conectar · Clic derecho para opciones");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setBorder(new EmptyBorder(6, 12, 6, 12));
        statusLabel.setBackground(HEADER_BG);
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.NORTH);

        // -- Contenido central: tabla + caminos --
        JPanel content = new JPanel(new GridLayout(1, 2, 1, 0));
        content.setBackground(BG);

        // Tabla de distancias
        tableModel = new DefaultTableModel(new Object[]{"Nodo", "Distancia mínima"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        distanceTable = new JTable(tableModel);
        styleTable();
        tableScroll = new JScrollPane(distanceTable);
        tableScroll.setBackground(PANEL_BG);
        tableScroll.getViewport().setBackground(PANEL_BG);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel tableContainer = createSection("📊  Tabla de distancias", tableScroll);
        content.add(tableContainer);

        // Panel de caminos
        pathsPanel = new JPanel();
        pathsPanel.setLayout(new BoxLayout(pathsPanel, BoxLayout.Y_AXIS));
        pathsPanel.setBackground(PANEL_BG);
        JScrollPane pathsScroll = new JScrollPane(pathsPanel);
        pathsScroll.setBackground(PANEL_BG);
        pathsScroll.getViewport().setBackground(PANEL_BG);
        pathsScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel pathsContainer = createSection("🗺  Caminos encontrados", pathsScroll);
        content.add(pathsContainer);

        add(content, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------ //
    //  API pública
    // ------------------------------------------------------------------ //

    /** Muestra los resultados de una ejecución de Dijkstra. */
    public void showResult(DijkstraResult result) {
        populateDistanceTable(result);
        populatePaths(result);

        String src  = result.getSource().getName();
        String dest = result.hasPath() ? result.getOptimalPath().getLast().getName() : "?";
        String cost = result.hasPath() ? String.valueOf(result.getOptimalCost()) : "∞";
        statusLabel.setText("  Dijkstra: " + src + " → " + dest
                            + "  |  Costo óptimo: " + cost
                            + "  |  Caminos alternativos: " + result.getSecondaryPaths().size());
        statusLabel.setForeground(OPTIMAL_CLR);
    }

    /** Muestra un mensaje de error en la barra de estado. */
    public void showError(String message) {
        statusLabel.setText("  ⚠  " + message);
        statusLabel.setForeground(ERROR_CLR);
    }

    /** Limpia la tabla y los caminos, restaura el mensaje por defecto. */
    public void clear() {
        tableModel.setRowCount(0);
        pathsPanel.removeAll();
        pathsPanel.revalidate();
        pathsPanel.repaint();
        statusLabel.setText("  Doble clic para agregar nodos · Arrastra entre nodos para conectar · Clic derecho para opciones");
        statusLabel.setForeground(TEXT_MUTED);
    }

    // ------------------------------------------------------------------ //
    //  Población de componentes
    // ------------------------------------------------------------------ //

    private void populateDistanceTable(DijkstraResult result) {
        tableModel.setRowCount(0);
        for (Map.Entry<Vertex, Integer> entry : result.getDistances().entrySet()) {
            String dist = entry.getValue() == Integer.MAX_VALUE
                ? "∞ (inalcanzable)" : String.valueOf(entry.getValue());
            tableModel.addRow(new Object[]{entry.getKey().getName(), dist});
        }
    }

    private void populatePaths(DijkstraResult result) {
        pathsPanel.removeAll();

        // Camino óptimo
        if (result.hasPath()) {
            pathsPanel.add(createPathLabel(
                "⭐  Óptimo",
                result.getOptimalPath(),
                result.getOptimalCost(),
                OPTIMAL_CLR
            ));
        } else {
            JLabel noPath = new JLabel("  Sin camino disponible.");
            noPath.setFont(new Font("SansSerif", Font.ITALIC, 12));
            noPath.setForeground(ERROR_CLR);
            noPath.setBorder(new EmptyBorder(8, 12, 4, 12));
            pathsPanel.add(noPath);
        }

        // Caminos secundarios
        java.util.List<DijkstraResult.PathResult> secondary = result.getSecondaryPaths();
        if (secondary.isEmpty() && result.hasPath()) {
            JLabel none = new JLabel("  No se encontraron caminos alternativos.");
            none.setFont(new Font("SansSerif", Font.ITALIC, 12));
            none.setForeground(TEXT_MUTED);
            none.setBorder(new EmptyBorder(4, 12, 4, 12));
            pathsPanel.add(none);
        } else {
            for (int i = 0; i < secondary.size(); i++) {
                DijkstraResult.PathResult pr = secondary.get(i);
                Color color = SEC_COLORS[i % SEC_COLORS.length];
                pathsPanel.add(createPathLabel(
                    "↪  Alternativo " + (i + 1),
                    pr.getPath(),
                    pr.getCost(),
                    color
                ));
            }
        }

        pathsPanel.revalidate();
        pathsPanel.repaint();
    }

    // ------------------------------------------------------------------ //
    //  Builders de componentes
    // ------------------------------------------------------------------ //

    private JPanel createSection(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BG);

        JLabel header = new JLabel("  " + title);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setForeground(TEXT_PRIMARY);
        header.setBackground(HEADER_BG);
        header.setOpaque(true);
        header.setBorder(new EmptyBorder(5, 10, 5, 10));

        panel.add(header, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPathLabel(String tag, LinkedList<Vertex> path,
                                    int cost, Color color) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setBackground(PANEL_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel tagLabel = new JLabel(tag);
        tagLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        tagLabel.setForeground(color);

        String pathStr = path.stream()
            .map(Vertex::getName)
            .collect(Collectors.joining(" → "));

        JLabel pathLabel = new JLabel(pathStr);
        pathLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        pathLabel.setForeground(TEXT_PRIMARY);

        JLabel costLabel = new JLabel("(costo: " + cost + ")");
        costLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        costLabel.setForeground(TEXT_MUTED);

        row.add(tagLabel);
        row.add(pathLabel);
        row.add(costLabel);
        return row;
    }

    // ------------------------------------------------------------------ //
    //  Estilo de la tabla
    // ------------------------------------------------------------------ //

    private void styleTable() {
        distanceTable.setBackground(PANEL_BG);
        distanceTable.setForeground(TEXT_PRIMARY);
        distanceTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        distanceTable.setRowHeight(24);
        distanceTable.setGridColor(new Color(40, 45, 70));
        distanceTable.setSelectionBackground(TABLE_SELECT);
        distanceTable.setSelectionForeground(Color.WHITE);
        distanceTable.setShowVerticalLines(false);
        distanceTable.setIntercellSpacing(new Dimension(0, 1));

        // Header
        distanceTable.getTableHeader().setBackground(HEADER_BG);
        distanceTable.getTableHeader().setForeground(TEXT_PRIMARY);
        distanceTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        distanceTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        // Renderer alternado y centrado
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected)
                    setBackground(row % 2 == 0 ? TABLE_ROW_A : TABLE_ROW_B);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        distanceTable.getColumnModel().getColumn(0).setCellRenderer(renderer);
        distanceTable.getColumnModel().getColumn(1).setCellRenderer(renderer);
        distanceTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        distanceTable.getColumnModel().getColumn(1).setPreferredWidth(160);
    }
}
