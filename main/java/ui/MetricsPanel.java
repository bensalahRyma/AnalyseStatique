package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.google.gson.GsonBuilder;
import stats.MetricService;

public class MetricsPanel extends JPanel {
    private final JTable summaryTable = new JTable();
    private final DefaultTableModel summaryModel =
            new DefaultTableModel(new Object[]{"Métrique", "Valeur"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

    private final JList<String> topByMethods = new JList<>();
    private final JList<String> topByAttrs   = new JList<>();
    private final JList<String> intersection = new JList<>();
    private final JTextArea perClassArea = new JTextArea();

    private MetricService.Results results;

    public MetricsPanel() {
        super(new BorderLayout(8, 8));

        // Résumé (table)
        summaryTable.setModel(summaryModel);
        summaryTable.setRowHeight(22);
        summaryTable.setFillsViewportHeight(true);

        // Listes
        JPanel lists = new JPanel(new GridLayout(1, 3, 8, 8));
        lists.add(wrap("Top 10% (# méthodes)", new JScrollPane(topByMethods)));
        lists.add(wrap("Top 10% (# attributs)", new JScrollPane(topByAttrs)));
        lists.add(wrap("Intersection", new JScrollPane(intersection)));

        // Détails par classe (monospace)
        perClassArea.setEditable(false);
        perClassArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JSplitPane top = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                wrap("Résumé", new JScrollPane(summaryTable)),
                lists);
        top.setResizeWeight(0.35);

        JSplitPane all = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                top,
                wrap("Top 10% méthodes par classe", new JScrollPane(perClassArea)));
        all.setResizeWeight(0.60);

        add(all, BorderLayout.CENTER);

        // Barre d’outils
        JToolBar tb = new JToolBar(); tb.setFloatable(false);
        JButton export = new JButton("Exporter JSON");
        export.addActionListener(e -> exportJson());
        tb.add(export);
        add(tb, BorderLayout.NORTH);
    }

    private JPanel wrap(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public void showResults(MetricService.Results r) {
        results = r;

        // Résumé
        summaryModel.setRowCount(0);
        addRow("Classes", r.classCount);
        addRow("Méthodes", r.methodCount);
        addRow("Packages", r.packageCount);
        addRow("LOC total", r.totalLoc);
        addRow("Max paramètres", r.maxParams);
        addRow("Moy. méthodes / classe", fmt(r.avgMethPerClass));
        addRow("Moy. attributs / classe", fmt(r.avgAttrPerClass));
        addRow("Moy. LOC / méthode", fmt(r.avgLocPerMethod));

        // Listes
        topByMethods.setListData(toArray(r.top10pByMethods));
        topByAttrs.setListData(toArray(r.top10pByAttrs));
        intersection.setListData(toArray(r.intersection));

        // Détails par classe
        String perClass = r.top10pMethodsByLocPerClass.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> "• " + e.getKey() + "\n  - " + String.join("\n  - ", e.getValue()))
                .collect(Collectors.joining("\n\n"));
        perClassArea.setText(perClass);
        perClassArea.setCaretPosition(0);
    }

    private void exportJson() {
        if (results == null) return;
        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JTextArea txt = new JTextArea(gson.toJson(results));
        txt.setEditable(false);
        txt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(txt),
                "JSON des statistiques", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addRow(String k, Object v) { summaryModel.addRow(new Object[]{k, String.valueOf(v)}); }
    private String[] toArray(List<String> list){ return list==null ? new String[0] : list.toArray(String[]::new); }
    private String fmt(double d){ return String.format("%.2f", d); }
}
