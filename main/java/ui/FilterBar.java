package ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterBar extends JPanel {
    public record Filter(Set<String> packages, Set<String> classes, int xThreshold) {}

    public interface Listener { void onApply(Filter f); }

    private final JList<String> pkgList = new JList<>();
    private final JList<String> clsList = new JList<>();
    private final JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(20, 0, 1000, 1));

    public FilterBar(List<String> allPackages, List<String> allClasses, int initialX, Listener listener) {
        super(new BorderLayout(8,8));
        setBorder(BorderFactory.createTitledBorder("Filtre d'analyse"));

        // listes multi-sélection
        pkgList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        clsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        pkgList.setVisibleRowCount(10);
        clsList.setVisibleRowCount(10);

        pkgList.setListData(allPackages.toArray(String[]::new));
        clsList.setListData(allClasses.toArray(String[]::new));
        xSpinner.setValue(initialX);

        // boutons rapides
        JButton pkAll = new JButton("Tout");  JButton pkNone = new JButton("Aucun");
        JButton clAll = new JButton("Tout");  JButton clNone = new JButton("Aucun");
        JButton apply = new JButton("Appliquer");

        pkAll.addActionListener(e -> pkgList.setSelectionInterval(0, Math.max(0, pkgList.getModel().getSize()-1)));
        pkNone.addActionListener(e -> pkgList.clearSelection());
        clAll.addActionListener(e -> clsList.setSelectionInterval(0, Math.max(0, clsList.getModel().getSize()-1)));
        clNone.addActionListener(e -> clsList.clearSelection());
        apply.addActionListener(e -> {
            Set<String> pkgs = pkgList.getSelectedValuesList().stream().collect(Collectors.toSet());
            Set<String> clss = clsList.getSelectedValuesList().stream().collect(Collectors.toSet());
            int x = (Integer) xSpinner.getValue();
            if (listener != null) listener.onApply(new Filter(pkgs, clss, x));
        });

        // layout
        JPanel left = new JPanel(new BorderLayout(4,4));
        left.setBorder(BorderFactory.createTitledBorder("Packages"));
        left.add(new JScrollPane(pkgList), BorderLayout.CENTER);
        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        leftBtns.add(pkAll); leftBtns.add(pkNone);
        left.add(leftBtns, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(4,4));
        right.setBorder(BorderFactory.createTitledBorder("Classes"));
        right.add(new JScrollPane(clsList), BorderLayout.CENTER);
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightBtns.add(clAll); rightBtns.add(clNone);
        right.add(rightBtns, BorderLayout.SOUTH);

        JPanel top = new JPanel(new GridLayout(1,2,8,8));
        top.add(left); top.add(right);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Seuil X (> méthodes) :"));
        bottom.add(xSpinner);
        bottom.add(Box.createHorizontalStrut(12));
        bottom.add(apply);

        add(top, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}
