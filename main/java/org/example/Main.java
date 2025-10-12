package org.example;

import graph.CallGraphBuilder;
import graph.CallGraphToGraphStream;
import model.ClassInfo;
import parser.EntityCollector;
import parser.JdtProject;
import stats.MetricService;
import ui.FilterBar;
import ui.GraphPanel;
import ui.MetricsPanel;
import utils.Args;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {
        var a = Args.parse(args);
        Path src = Paths.get(a.getOrDefault("src", "."));
        int X = Integer.parseInt(a.getOrDefault("x", "20"));
        List<String> cp = a.containsKey("cp") ? List.of(a.get("cp").split("[;:]")) : List.of();

        // 1) Parse
        JdtProject proj = JdtProject.fromSourceDir(src, cp);
        var units = proj.units();

        // 2) Collect global classes/packages (pour la UI)
        var ec = new EntityCollector();
        for (var pu : units) pu.cu.accept(ec);
        Map<String, ClassInfo> allClasses = ec.classes();
        List<String> allPkgs = allClasses.values().stream()
                .map(c -> c.packageName == null ? "" : c.packageName)
                .distinct().sorted().collect(Collectors.toList());
        List<String> allCls = allClasses.values().stream()
                .map(c -> c.qualifiedName).sorted().collect(Collectors.toList());

        // 3) Builder du graphe (complet, filtrage au moment de l’export)
        var builder = new CallGraphBuilder();
        for (var pu : units) pu.cu.accept(builder);

        // 4) UI Swing
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Analyse Statique (JDT) — Java 21");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setLayout(new BorderLayout());

            var metricsPanel = new MetricsPanel();
            var graphPanel = new GraphPanel();

            // Panneau central : onglets
            var tabs = new JTabbedPane();
            tabs.addTab("Statistiques", metricsPanel);
            tabs.addTab("Graphe d'appel", graphPanel);

            // Panneau latéral : filtres
            var filterBar = new FilterBar(allPkgs, allCls, X, flt -> {
                try {
                    // Recalcul métriques
                    var svc = new MetricService();
                    var results = svc.compute(units, flt.xThreshold(), flt.packages(), flt.classes());
                    metricsPanel.showResults(results);


                    // Graphe filtré
                    var gsGraph = CallGraphToGraphStream.toGraph(builder, flt.packages(), flt.classes());
                    graphPanel.setGraph(gsGraph);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(f, "Erreur: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            });

            // Premier affichage (sans filtre)
            var svc = new MetricService();
            var results = svc.compute(units, X);
            metricsPanel.showResults(results);
            var gsGraph = CallGraphToGraphStream.toGraph(builder, Collections.emptySet(), Collections.emptySet());
            graphPanel.setGraph(gsGraph);

            // Layout final
            f.add(filterBar, BorderLayout.WEST);
            f.add(tabs, BorderLayout.CENTER);
            f.setSize(new Dimension(1250, 750));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
