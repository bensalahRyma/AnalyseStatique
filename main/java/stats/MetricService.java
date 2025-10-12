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

    /** API existante (pas de filtre) */
    public Results compute(List<JdtProject.ParsedUnit> units, int thresholdX) {
        return compute(units, thresholdX, Collections.emptySet(), Collections.emptySet());
    }

    /** Nouveau: compute avec filtres packages/classes (OR logique) */
    public Results compute(List<JdtProject.ParsedUnit> units, int thresholdX,
                           Set<String> includePackages, Set<String> includeClasses) {
        // 1) Collecte sur toutes les CUs
        EntityCollector ec = new EntityCollector();
        for (JdtProject.ParsedUnit pu : units) {
            CompilationUnit cu = pu.cu; // ParsedUnit avec champs publics
            cu.accept(ec);
        }
        Map<String, ClassInfo> allClasses = ec.classes();

        // 2) Filtrage (owner in classes) OR (package in packages)
        final Set<String> pkgsSel = includePackages == null ? Collections.emptySet() : includePackages;
        final Set<String> clsSel  = includeClasses  == null ? Collections.emptySet() : includeClasses;

        java.util.function.Predicate<ClassInfo> accept = c -> {
            boolean classOk = !clsSel.isEmpty() && clsSel.contains(c.qualifiedName);
            boolean pkgOk   = pkgsSel.isEmpty() || pkgsSel.contains(c.packageName == null ? "" : c.packageName);
            return classOk || pkgOk;
        };

        Map<String, ClassInfo> classes = allClasses.entrySet().stream()
                .filter(e -> accept.test(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 3) Ensemble des packages retenus
        Set<String> packages = classes.values().stream()
                .map(c -> c.packageName == null ? "" : c.packageName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 4) Calculs
        int totalAttrs   = classes.values().stream().mapToInt(c -> c.attributeCount).sum();
        int totalMethods = classes.values().stream().mapToInt(c -> c.methods.size()).sum();

        // LOC total: somme des LOC des classes retenues (portion source de la classe)
        int totalLoc = 0;
        for (Map.Entry<String, ClassInfo> e : classes.entrySet()) {
            var ci = e.getValue();
            JdtProject.ParsedUnit pu = findUnitForClass(units, e.getKey());
            if (pu != null && ci.srcLen > 0) {
                String slice = safeSlice(pu.source, ci.srcStart, ci.srcLen);
                totalLoc += Locs.countNonEmpty(slice);
            }
        }

        int totalMethodLoc = 0, mCount = 0, maxParams = 0;
        Map<String, List<String>> topMapLocal = new HashMap<>();

        // Top 10% des méthodes les plus longues (par classe)
        for (Map.Entry<String, ClassInfo> e : classes.entrySet()) {
            ClassInfo cls = e.getValue();
            List<ML> mls = new ArrayList<>();

            JdtProject.ParsedUnit pu = findUnitForClass(units, e.getKey());
            if (pu == null) continue;

            for (MethodInfo m : cls.methods) {
                String slice = safeSlice(pu.source, m.srcStart, m.srcLen);
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
                topMapLocal.put(e.getKey(), picked);
            }
        }

        // Top 10% classes par méthodes / attributs
        int kClasses = Math.max(1, (int) Math.ceil(Math.max(1, classes.size()) * 0.10));
        List<String> topByMethods = classes.values().stream()
                .sorted(Comparator.comparingInt((ClassInfo c) -> c.methods.size()).reversed())
                .limit(kClasses).map(c -> c.qualifiedName).collect(Collectors.toList());
        List<String> topByAttrs = classes.values().stream()
                .sorted(Comparator.comparingInt((ClassInfo c) -> c.attributeCount).reversed())
                .limit(kClasses).map(c -> c.qualifiedName).collect(Collectors.toList());
        List<String> inter = new ArrayList<>(topByMethods); inter.retainAll(topByAttrs);

        List<String> overX = classes.values().stream()
                .filter(c -> c.methods.size() > thresholdX)
                .map(c -> c.qualifiedName)
                .sorted()
                .collect(Collectors.toList());

        // 5) Résultats
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
        r.top10pMethodsByLocPerClass = topMapLocal;
        return r;
    }

    // ---------- helpers ----------

    private static JdtProject.ParsedUnit findUnitForClass(List<JdtProject.ParsedUnit> units, String fqcn) {
        String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        for (var u : units) {
            // heuristique simple : "class <SimpleName>"
            if (u.source.contains("class " + simple)
                    || u.source.contains("enum " + simple)
                    || u.source.contains("record " + simple)) {
                return u;
            }
        }
        return null;
    }

    private static String safeSlice(String src, int start, int len) {
        int s = Math.max(0, start);
        int e = Math.min(src.length(), start + Math.max(0, len));
        return (s < e) ? src.substring(s, e) : "";
    }
}
