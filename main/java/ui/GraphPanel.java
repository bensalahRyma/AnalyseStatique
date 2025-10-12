package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.swing_viewer.SwingViewer;   // GS 2.x (underscore)
import org.graphstream.ui.swing_viewer.ViewPanel;

public class GraphPanel extends JPanel {
    private Viewer viewer;
    private ViewPanel viewPanel;
    private Graph graph;

    private enum LabelMode { NONE, SIMPLE, FULL }
    private LabelMode labelMode = LabelMode.SIMPLE;
    private boolean autoLayout = true;

    // UI
    private final JTextField searchField = new JTextField(16);

    public GraphPanel() {
        super(new BorderLayout(6,6));

        // Toolbar
        JToolBar tb = new JToolBar(); tb.setFloatable(false);
        JButton btnZoomIn  = new JButton("+");
        JButton btnZoomOut = new JButton("−");
        JButton btnFit     = new JButton("Fit");
        JButton btnTree    = new JButton("Tree (Top-Down)");
        JCheckBox cbAuto   = new JCheckBox("AutoLayout", true);
        JComboBox<String> labelCombo = new JComboBox<>(new String[]{"Labels: Aucun","Labels: Simple","Labels: Complet"});
        JButton btnGo      = new JButton("Go");
        JButton btnClear   = new JButton("Clear");
        JButton btnPng     = new JButton("PNG");

        tb.add(btnZoomIn); tb.add(btnZoomOut); tb.add(btnFit); tb.addSeparator();
        tb.add(btnTree); tb.add(cbAuto); tb.addSeparator();
        tb.add(new JLabel("Rechercher: ")); tb.add(searchField); tb.add(btnGo); tb.add(btnClear);
        tb.add(Box.createHorizontalStrut(8)); tb.add(labelCombo);
        tb.add(Box.createHorizontalGlue()); tb.add(btnPng);
        add(tb, BorderLayout.NORTH);

        // actions
        btnZoomIn.addActionListener(e -> zoom(0.85));
        btnZoomOut.addActionListener(e -> zoom(1.18));
        btnFit.addActionListener(e -> fit());
        btnPng.addActionListener(e -> exportPng());
        btnGo.addActionListener(e -> highlight(searchField.getText().trim()));
        btnClear.addActionListener(e -> { searchField.setText(""); highlight(""); });
        labelCombo.addActionListener(e -> {
            switch (labelCombo.getSelectedIndex()) {
                case 0 -> setLabelMode(LabelMode.NONE);
                case 1 -> setLabelMode(LabelMode.SIMPLE);
                case 2 -> setLabelMode(LabelMode.FULL);
            }
        });
        cbAuto.addActionListener(e -> toggleAutoLayout(cbAuto.isSelected()));
        btnTree.addActionListener(e -> treeLayoutTopDown()); // ⬅️ le layout hiérarchique
    }

    /** Branche le graphe */
    public void setGraph(Graph g) {
        this.graph = g;
        applyStyle(graph);     // rectangles
        colorByPackage();      // couleurs par package
        if (viewPanel != null) remove(viewPanel);
        if (viewer != null) { try { viewer.close(); } catch (Exception ignored) {} }

        viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();
        autoLayout = true;

        viewPanel = (ViewPanel) viewer.addDefaultView(false);
        add(viewPanel, BorderLayout.CENTER);

        viewPanel.getCamera().setViewPercent(1.25);
        viewPanel.addMouseWheelListener(ev -> {
            double f = ev.getPreciseWheelRotation() > 0 ? 1.10 : 0.90;
            var cam = viewPanel.getCamera();
            cam.setViewPercent(cam.getViewPercent() * f);
        });

        // labels init
        setLabelMode(labelMode);

        revalidate(); repaint();
    }

    // ========== actions usuelles ==========

    private void zoom(double factor) {
        if (viewPanel == null) return;
        var cam = viewPanel.getCamera();
        cam.setViewPercent(cam.getViewPercent() * factor);
    }

    private void fit() {
        if (viewPanel == null) return;
        var cam = viewPanel.getCamera();
        cam.resetView();
        cam.setViewPercent(1.10);
    }

    private void toggleAutoLayout(boolean on) {
        autoLayout = on;
        if (viewer == null) return;
        if (on) viewer.enableAutoLayout(); else viewer.disableAutoLayout();
    }

    private void exportPng() {
        if (viewPanel == null) return;
        BufferedImage img = new BufferedImage(viewPanel.getWidth(), viewPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        viewPanel.printAll(g2);
        g2.dispose();

        JFileChooser ch = new JFileChooser();
        ch.setSelectedFile(new File("callgraph.png"));
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(img, "png", ch.getSelectedFile());
                JOptionPane.showMessageDialog(this, "PNG exporté : " + ch.getSelectedFile().getAbsolutePath(),
                        "Export PNG", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erreur export PNG : " + ex.getMessage(),
                        "Export PNG", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Recherche: surligne; préfixe '!' pour isoler nœud + voisinage 1-hop */
    private void highlight(String term) {
        if (graph == null) return;
        String q = term.toLowerCase(Locale.ROOT).trim();
        boolean isolate = q.startsWith("!");
        if (isolate) q = q.substring(1).trim();

        Set<String> matches = new HashSet<>();
        for (int i=0;i<graph.getNodeCount();i++) {
            Node n = graph.getNode(i);
            String lbl = String.valueOf(n.getAttribute("ui.label"));
            if (q.isEmpty()
                    || n.getId().toLowerCase(Locale.ROOT).contains(q)
                    || (lbl != null && lbl.toLowerCase(Locale.ROOT).contains(q))) {
                matches.add(n.getId());
            }
        }
        Set<String> allowed = new HashSet<>(matches);
        if (isolate && !matches.isEmpty()) {
            for (String id : new ArrayList<>(matches)) {
                Node n = graph.getNode(id);
                for (int j=0;j<n.getDegree();j++) {
                    Node other = n.getEdge(j).getOpposite(n);
                    if (other != null) allowed.add(other.getId());
                }
            }
        }
        for (int i=0;i<graph.getNodeCount();i++) {
            Node n = graph.getNode(i);
            if (q.isEmpty()) n.removeAttribute("ui.class");
            else n.setAttribute("ui.class", allowed.contains(n.getId()) ? "match" : "dim");
        }
    }

    private void setLabelMode(LabelMode mode) {
        labelMode = mode;
        if (graph == null) return;
        for (int i=0;i<graph.getNodeCount();i++) {
            Node n = graph.getNode(i);
            switch (labelMode) {
                case NONE   -> n.setAttribute("ui.label", "");
                case SIMPLE -> n.setAttribute("ui.label", simpleLabel(n.getId()));
                case FULL   -> n.setAttribute("ui.label", n.getId());
            }
            n.setAttribute("ui.tooltip", n.getId());
        }
    }

    // ========== LAYOUT HIERARCHIQUE TOP-DOWN ==========

    /** Place les nœuds en niveaux (type organigramme) et fige le layout. */
    private void treeLayoutTopDown() {
        if (graph == null) return;
        toggleAutoLayout(false); // on fige

        // 1) calcul in-degree (directed)
        Map<Node,Integer> indeg = new HashMap<>();
        Map<Node,List<Node>> out = new HashMap<>();
        for (int i=0;i<graph.getNodeCount();i++) {
            Node n = graph.getNode(i);
            indeg.put(n, 0);
            out.put(n, new ArrayList<>());
        }
        for (int i=0;i<graph.getEdgeCount();i++) {
            Edge e = graph.getEdge(i);
            Node s = e.isDirected() ? e.getSourceNode() : e.getNode0();
            Node t = e.isDirected() ? e.getTargetNode() : e.getNode1();
            if (s == null || t == null) continue;
            out.get(s).add(t);
            indeg.put(t, indeg.get(t) + 1);
        }

        // 2) sources (racines)
        Deque<Node> q = new ArrayDeque<>();
        for (var n : indeg.keySet()) if (indeg.get(n) == 0) q.add(n);
        // si graphe fermé (cycle complet), on prend un nœud quelconque
        if (q.isEmpty() && graph.getNodeCount()>0) q.add(graph.getNode(0));

        // 3) BFS par couches (Kahn)
        Map<Node,Integer> level = new HashMap<>();
        List<List<Node>> layers = new ArrayList<>();
        while (!q.isEmpty()) {
            Node u = q.removeFirst();
            int L = level.getOrDefault(u, 0);
            while (layers.size() <= L) layers.add(new ArrayList<>());
            layers.get(L).add(u);
            for (Node v : out.get(u)) {
                int d = indeg.merge(v, -1, Integer::sum);
                if (d == 0) {
                    level.put(v, L+1);
                    q.addLast(v);
                } else if (!level.containsKey(v)) {
                    // si cycle, on force un niveau au moins égal
                    level.put(v, Math.max(L+1, level.getOrDefault(v, L+1)));
                }
            }
        }
        // nœuds restants (cycles) -> dernier niveau
        for (int i=0;i<graph.getNodeCount();i++) {
            Node n = graph.getNode(i);
            if (!level.containsKey(n)) {
                int L = layers.size();
                while (layers.size() <= L) layers.add(new ArrayList<>());
                layers.get(L).add(n);
                level.put(n, L);
            }
        }

        // 4) placement: niveaux de haut en bas, espacements réguliers
        double dx = 30.0;      // espacement horizontal
        double dy = 25.0;      // espacement vertical
        double x0 = 0.0, y0 = 0.0; // origine

        for (int L=0; L<layers.size(); L++) {
            List<Node> row = layers.get(L);
            // petit tri esthétique : par nom simple
            row.sort(Comparator.comparing(n -> simpleLabel(n.getId()).toLowerCase(Locale.ROOT)));

            // centrer la rangée
            double width = (row.size()-1) * dx;
            double x = x0 - width/2.0;
            double y = y0 + L * dy;

            for (Node n : row) {
                n.setAttribute("xyz", x, -y, 0); // y négatif -> top-down
                x += dx;
            }
        }
    }

    // ========== style & helpers ==========

    private void applyStyle(Graph g) {
        g.setAttribute("ui.stylesheet",
                "graph { padding: 60px; } " +
                        "node { " +
                        "  shape: box; size-mode: fit; padding: 6px, 10px; " +
                        "  text-size: 12px; text-alignment: center; text-style: bold; " +
                        "  fill-color: #f7f7f7; stroke-mode: plain; stroke-color: #444; stroke-width: 1.0px; " +
                        "} " +
                        "node.match { stroke-color: #000; stroke-width: 2.0px; text-size: 14px; } " +
                        "node.dim { text-color: #888; } " +
                        "edge { arrow-shape: arrow; arrow-size: 8px,6px; }"
        );
        g.setAttribute("ui.quality");
        g.setAttribute("ui.antialias");
    }

    private void colorByPackage() {
        if (graph == null) return;
        for (int i=0;i<graph.getNodeCount();i++) {
            Node n = graph.getNode(i);
            String pkg = pkgOf(n.getId());
            String color = colorFor(pkg);
            String base = (String) n.getAttribute("ui.style");
            n.setAttribute("ui.style", mergeStyle(base, "fill-color: " + color + ";"));
            if (labelMode == LabelMode.SIMPLE && n.getAttribute("ui.label") == null)
                n.setAttribute("ui.label", simpleLabel(n.getId()));
        }
    }

    private static String simpleLabel(String id) {
        int p = id.indexOf('(');
        int dot = (p > 0) ? id.lastIndexOf('.', p) : id.lastIndexOf('.');
        if (dot <= 0) return id;
        String owner = id.substring(0, dot);
        String meth  = id.substring(dot+1);
        String simpleOwner = owner.contains(".") ? owner.substring(owner.lastIndexOf('.')+1) : owner;
        return simpleOwner + "." + meth;
    }

    private static String pkgOf(String id) {
        int p = id.indexOf('(');
        int dot = (p > 0) ? id.lastIndexOf('.', p) : id.lastIndexOf('.');
        if (dot <= 0) return "";
        String owner = id.substring(0, dot);
        int i = owner.lastIndexOf('.');
        return (i > 0) ? owner.substring(0, i) : "";
    }

    private static String colorFor(String pkg) {
        int h = pkg.hashCode();
        int r = 0x40 + Math.floorMod(h, 0xC0);
        int g = 0x40 + Math.floorMod(h >> 8, 0xC0);
        int b = 0x40 + Math.floorMod(h >> 16, 0xC0);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private static String mergeStyle(String base, String extra) {
        if (base == null || base.isBlank()) return extra;
        if (extra == null || extra.isBlank()) return base;
        String b = base.trim(), e = extra.trim();
        if (e.contains("fill-color:"))  b = b.replaceAll("fill-color:[^;]*;", "");
        if (e.contains("stroke-width:")) b = b.replaceAll("stroke-width:[^;]*;", "");
        if (!b.endsWith(";")) b += ";";
        return b + " " + e;
    }
}
