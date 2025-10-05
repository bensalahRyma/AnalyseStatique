package ui;
import javax.swing.*; import java.awt.*;

import org.graphstream.graph.Node;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.swing_viewer.SwingViewer;   // ✅ bon package

import org.graphstream.ui.view.Viewer;
import org.graphstream.graph.Graph;
import org.graphstream.ui.view.camera.Camera;

public class GraphPanel extends JPanel {
    private Viewer viewer;
    private ViewPanel viewPanel;
    private Graph graph;
    private boolean labelsVisible = true;

    public GraphPanel() {
        super(new BorderLayout());

        JToolBar tb = new JToolBar(); tb.setFloatable(false);
        JButton zoomIn = new JButton("+");
        JButton zoomOut = new JButton("−");
        JButton fit = new JButton("Fit");
        JButton toggle = new JButton("Labels");
        tb.add(zoomIn); tb.add(zoomOut); tb.add(fit); tb.addSeparator(); tb.add(toggle);
        add(tb, BorderLayout.NORTH);

        zoomIn.addActionListener(e -> zoom(0.8));
        zoomOut.addActionListener(e -> zoom(1.25));
        fit.addActionListener(e -> fit());
        toggle.addActionListener(e -> toggleLabels());
    }

    public void setGraph(Graph g) {
        this.graph = g;
        applyStyle(graph);

        if (viewPanel != null) remove(viewPanel);
        if (viewer != null) { try { viewer.close(); } catch (Exception ignored) {} }

        viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();

        viewPanel = (ViewPanel) viewer.addDefaultView(false);
        add(viewPanel, BorderLayout.CENTER);

        viewPanel.getCamera().setViewPercent(1.5);
        revalidate(); repaint();
    }

    private void zoom(double factor) {
        if (viewPanel == null) return;
        var cam = viewPanel.getCamera();
        cam.setViewPercent(cam.getViewPercent() * factor);
    }

    private void fit() {
        if (viewPanel == null) return;
        var cam = viewPanel.getCamera();
        cam.resetView();
        cam.setViewPercent(1.2);
    }

    // ✅ FIX: itération portable sans getEachNode()/nodes()
    private void toggleLabels() {
        if (graph == null) return;
        labelsVisible = !labelsVisible;

        for (Node n : graph) { // Graph est Iterable<Node>
            Object lbl = n.getAttribute("ui.label");
            if (labelsVisible) {
                n.setAttribute("ui.label", (lbl != null) ? lbl : n.getId());
            } else {
                n.setAttribute("ui.label", "");
            }
        }
    }

    private void applyStyle(Graph g) {
        g.setAttribute("ui.stylesheet",
                "graph { padding: 60px; } " +
                        "node { size: 7px; fill-color: #222; text-size: 12px; text-alignment: at-right; } " +
                        "edge { arrow-shape: arrow; arrow-size: 8px,6px; }");
        g.setAttribute("ui.quality");
        g.setAttribute("ui.antialias");
    }
}