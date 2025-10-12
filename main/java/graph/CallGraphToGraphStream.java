package graph;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.Set;

public class CallGraphToGraphStream {

    public static Graph toGraph(CallGraphBuilder builder,
                                Set<String> includePackages,
                                Set<String> includeClasses) {
        Graph g = new SingleGraph("CallGraph");

        java.util.function.Predicate<String> acceptOwner = owner -> {
            if (owner == null) return false;
            if (includeClasses != null && !includeClasses.isEmpty() && includeClasses.contains(owner))
                return true;
            String pkg = owner.contains(".") ? owner.substring(0, owner.lastIndexOf('.')) : "";
            return includePackages == null || includePackages.isEmpty() || includePackages.contains(pkg);
        };

        builder.graph().forEach((from, tos) -> {
            if (!acceptOwner.test(from.owner())) return;
            String fid = (from.owner()+"."+from.nameSig()).replaceAll("\\s+","");
            ensureNode(g, fid, shortLabel(from));
            for (var t : tos) {
                if (!acceptOwner.test(t.owner())) continue;
                String tid = (t.owner()+"."+t.nameSig()).replaceAll("\\s+","");
                ensureNode(g, tid, shortLabel(t));
                String eid = fid + "->" + tid;
                if (g.getEdge(eid) == null) g.addEdge(eid, fid, tid, true);
            }
        });

        return g;
    }

    private static void ensureNode(Graph g, String id, String label) {
        if (g.getNode(id) == null) g.addNode(id).setAttribute("ui.label", label);
    }
    private static String shortLabel(CallGraphBuilder.MethodKey k) {
        String owner = k.owner();
        int i = owner.lastIndexOf('.');
        String simple = (i>=0 ? owner.substring(i+1) : owner);
        return simple + "." + k.nameSig();
    }
    private static String edgeId(String from, String to) {
        return from + "->" + to;
    }

    private static String nodeId(CallGraphBuilder.MethodKey k) {
        // id lisible et stable ; on enlève les espaces superflus
        return (k.owner() + "." + k.nameSig()).replaceAll("\\s+", "");
    }
}
