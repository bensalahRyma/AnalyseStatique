package org.example;

import graph.CallGraphBuilder;
import graph.CallGraphToGraphStream;
import parser.JdtProject;
import stats.MetricService;
import ui.GraphPanel;
import ui.MetricsPanel;
import utils.Args;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;     // ✅ le bon Path
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        var a = Args.parse(args);

        Path src = Paths.get(a.getOrDefault("src", "."));
        int X = Integer.parseInt(a.getOrDefault("x", "20"));
        List<String> cp = a.containsKey("cp") ? List.of(a.get("cp").split("[;:]")) : List.of();

        // 1) Parse + collect
        JdtProject proj = JdtProject.fromSourceDir(src, cp);
        var units = proj.units();

        // 2) Stats
        var svc = new MetricService();
        var results = svc.compute(units, X);

        // 3) Graphe d'appel
        var builder = new CallGraphBuilder();
        for (var pu : units) pu.cu.accept(builder);
        var gsGraph = CallGraphToGraphStream.toGraph(builder);

        // 4) UI Swing
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Analyse Statique (JDT) — Java 21");
            var tabs = new JTabbedPane();
            var metricsPanel = new MetricsPanel(); metricsPanel.showResults(results);
            var graphPanel = new GraphPanel(); graphPanel.setGraph(gsGraph);
            tabs.addTab("Statistiques", metricsPanel);
            tabs.addTab("Graphe d'appel", graphPanel);
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setContentPane(tabs);
            f.setSize(new Dimension(1100, 700));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
