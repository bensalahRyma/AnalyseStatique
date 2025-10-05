package graph;

import org.eclipse.jdt.core.dom.*;
import java.util.*;
public class CallGraphBuilder extends ASTVisitor {
    public static record MethodKey(String owner, String nameSig) {}
    private final Deque<MethodKey> ctx = new ArrayDeque<>();
    private final Map<MethodKey, Set<MethodKey>> edges = new HashMap<>();
    public Map<MethodKey, Set<MethodKey>> graph(){ return edges; }

    @Override public boolean visit(MethodDeclaration n){
        IMethodBinding mb = n.resolveBinding(); if (mb==null) return true;
        String owner = mb.getDeclaringClass()!=null? mb.getDeclaringClass().getQualifiedName() : "<anon>";
        String sig = mb.getName()+"("+String.join(",", Arrays.stream(mb.getParameterTypes()).map(ITypeBinding::getName).toArray(String[]::new))+")";
        ctx.push(new MethodKey(owner, sig)); return true;
    }
    @Override public void endVisit(MethodDeclaration n){
        if (!ctx.isEmpty()) ctx.pop();
    }

    private static boolean isProjectType(String qn) {
        if (qn == null) return false;
        return !(qn.startsWith("java.")
                || qn.startsWith("javax.")
                || qn.startsWith("jdk.")
                || qn.startsWith("org.eclipse.")
                || qn.startsWith("org.graphstream."));
    }

    private void edgeTo(IMethodBinding target) {
        if (target == null || ctx.isEmpty()) return;
        var caller = ctx.peek();

        String toOwner = target.getDeclaringClass()!=null ? target.getDeclaringClass().getQualifiedName() : null;
        if (!isProjectType(caller.owner()) || !isProjectType(toOwner)) return; // <-- filtre

        String toSig = target.getName() + "(" +
                Arrays.stream(target.getParameterTypes()).map(ITypeBinding::getName).reduce((a,b)->a+","+b).orElse("") + ")";
        var callee = new MethodKey(toOwner, toSig);
        edges.computeIfAbsent(caller, k -> new HashSet<>()).add(callee);
    }
    @Override public boolean visit(MethodInvocation n){
        edgeTo(n.resolveMethodBinding());
        return true;
    }
    @Override public boolean visit(SuperMethodInvocation n){
        edgeTo(n.resolveMethodBinding());
        return true;
    }
    @Override public boolean visit(ClassInstanceCreation n){
        edgeTo(n.resolveConstructorBinding());
        return true;
    }
}
