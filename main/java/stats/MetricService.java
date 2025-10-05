package stats;

import java.util.*;
import java.util.stream.Collectors;

import model.ClassInfo;
import model.MethodInfo;
import org.eclipse.jdt.core.dom.CompilationUnit;
import parser.EntityCollector;
import parser.JdtProject;
import utils.Locs;

public class MetricService {
    public static class Results {
        public int classCount, methodCount, packageCount, totalLoc, maxParams;
        public double avgMethPerClass, avgLocPerMethod, avgAttrPerClass;
        public List<String> top10pByMethods, top10pByAttrs, intersection, overXMethods;
        public Map<String, List<String>> top10pMethodsByLocPerClass = new HashMap<>();
    }

    private static final class ML {
        final String sig; final int loc;
        ML(String sig, int loc) { this.sig = sig; this.loc = loc; }
    }

    private final Map<String, List<String>> topMap = new HashMap<>();
    private Map<String, List<String>> resultsTopMap() { return topMap; }

    public Results compute(List<JdtProject.ParsedUnit> units, int thresholdX) {
        EntityCollector ec = new EntityCollector();
        Set<String> packages = new HashSet<>();

        // Parcours des CUs pour remplir le collecteur et les packages
        for (JdtProject.ParsedUnit pu : units) {
            CompilationUnit cu = pu.cu; // ParsedUnit avec champs publics
            packages.add(
                    Optional.ofNullable(cu.getPackage())
                            .map(p -> p.getName().getFullyQualifiedName())
                            .orElse("")
            );
            cu.accept(ec);
        }

        // Récupération des classes collectées
        Map<String, ClassInfo> classes = ec.classes();

        int totalAttrs   = classes.values().stream().mapToInt(c -> c.attributeCount).sum();
        int totalMethods = classes.values().stream().mapToInt(c -> c.methods.size()).sum();
        int totalLoc     = units.stream().mapToInt(u -> Locs.countNonEmpty(u.source)).sum();

        int totalMethodLoc = 0, mCount = 0, maxParams = 0;

        // Top 10% des méthodes les plus longues (par classe)
        for (Map.Entry<String, ClassInfo> e : classes.entrySet()) {
            ClassInfo cls = e.getValue();
            List<ML> mls = new ArrayList<>();

            // Trouver l'unité source correspondante (heuristique simple par nom de classe)
            JdtProject.ParsedUnit pu = units.stream()
                    .filter(u -> u.source.contains("class " + e.getKey().substring(e.getKey().lastIndexOf('.') + 1)))
                    .findFirst().orElse(null);
            if (pu == null) continue;

            for (MethodInfo m : cls.methods) {
                String slice = pu.source.substring(m.srcStart, m.srcStart + m.srcLen);
                int loc = Locs.countNonEmpty(slice);
                mls.add(new ML(m.signature, loc));
                totalMethodLoc += loc;
                mCount++;
                maxParams = Math.max(maxParams, m.paramCount);
            }

            int kMethodsInClass = Math.max(1, (int) Math.ceil(mls.size() * 0.10));
            mls.sort(Comparator.comparingInt(x -> -x.loc));

            List<String> picked = mls.subList(0, Math.min(kMethodsInClass, mls.size()))
                    .stream()
                    .map(x -> x.sig + " [" + x.loc + " LOC]")
                    .collect(Collectors.toList());

            if (!picked.isEmpty()) {
                resultsTopMap().put(e.getKey(), picked);
            }
        }

        // Top 10% classes par méthodes / attributs
        int kClasses = Math.max(1, (int) Math.ceil(Math.max(1, classes.size()) * 0.10));

        List<String> topByMethods = classes.values().stream()
                .sorted(Comparator.comparingInt((ClassInfo c) -> c.methods.size()).reversed())
                .limit(kClasses)
                .map(c -> c.qualifiedName)
                .collect(Collectors.toList());

        List<String> topByAttrs = classes.values().stream()
                .sorted(Comparator.comparingInt((ClassInfo c) -> c.attributeCount).reversed())
                .limit(kClasses)
                .map(c -> c.qualifiedName)
                .collect(Collectors.toList());

        List<String> inter = new ArrayList<>(topByMethods);
        inter.retainAll(topByAttrs);

        List<String> overX = classes.values().stream()
                .filter(c -> c.methods.size() > thresholdX)
                .map(c -> c.qualifiedName)
                .sorted()
                .collect(Collectors.toList());

        // Résultats
        Results r = new Results();
        r.classCount = classes.size();
        r.methodCount = totalMethods;
        r.packageCount = packages.size();
        r.totalLoc = totalLoc;
        r.maxParams = maxParams;
        r.avgMethPerClass = r.classCount == 0 ? 0 : (double) r.methodCount / r.classCount;
        r.avgAttrPerClass = r.classCount == 0 ? 0 : (double) totalAttrs / r.classCount;
        r.avgLocPerMethod = mCount == 0 ? 0 : (double) totalMethodLoc / mCount;
        r.top10pByMethods = topByMethods;
        r.top10pByAttrs = topByAttrs;
        r.intersection = inter;
        r.overXMethods = overX;
        r.top10pMethodsByLocPerClass = resultsTopMap();
        return r;
    }
}
