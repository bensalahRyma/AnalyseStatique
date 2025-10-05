package graph;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

public class CallGraphToGraphStream {

    public static Graph toGraph(CallGraphBuilder builder) {
        Graph g = new SingleGraph("CallGraph");

        builder.graph().forEach((from, tos) -> {
            String f = nodeId(from);
            ensureNode(g, f);

            for (var t : tos) {
                String tt = nodeId(t);
                ensureNode(g, tt);

                String eid = edgeId(f, tt);
                if (g.getEdge(eid) == null) {
                    g.addEdge(eid, f, tt, true);

                }
            }
        });

        return g;
    }


    private static void ensureNode(Graph g, String id) {
        if (g.getNode(id) == null) {
            g.addNode(id).setAttribute("ui.label", id);
        }
    }

    private static String edgeId(String from, String to) {
        return from + "->" + to;
    }

    private static String nodeId(CallGraphBuilder.MethodKey k) {
        // id lisible et stable ; on enlève les espaces superflus
        return (k.owner() + "." + k.nameSig()).replaceAll("\\s+", "");
    }
}
