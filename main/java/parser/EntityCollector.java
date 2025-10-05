package parser;

import java.util.*;

import model.ClassInfo;
import model.MethodInfo;
import org.eclipse.jdt.core.dom.*;
public class EntityCollector extends ASTVisitor {
    private final Map<String, ClassInfo> classes = new HashMap<>();
    private String currentPkg = "";
    public Map<String, ClassInfo> classes(){ return classes; }

    @Override public boolean visit(PackageDeclaration n){
        currentPkg = n.getName().getFullyQualifiedName(); return true;
    }
    @Override public boolean visit(TypeDeclaration n){
        if (n.isInterface()) return true;
        var ci = new ClassInfo();
        ci.packageName = currentPkg;
        ci.qualifiedName = (currentPkg.isEmpty()? "" : currentPkg + ".") + n.getName().getIdentifier();
        ci.srcStart = n.getStartPosition(); ci.srcLen = n.getLength();
        classes.put(ci.qualifiedName, ci);
        return true;
    }
    @Override public boolean visit(FieldDeclaration n){
        var td = (TypeDeclaration)n.getParent(); if (td==null || td.isInterface()) return false;
        String qn = (currentPkg.isEmpty()? "" : currentPkg + ".") + td.getName().getIdentifier();
        classes.get(qn).attributeCount += n.fragments().size();
        return false;
    }
    @Override public boolean visit(MethodDeclaration n){
        var td = (TypeDeclaration)n.getParent(); if (td==null || td.isInterface()) return false;
        String qn = (currentPkg.isEmpty()? "" : currentPkg + ".") + td.getName().getIdentifier();
        var mi = new MethodInfo();
        mi.name = n.getName().getIdentifier();
        mi.paramCount = n.parameters().size();
        mi.srcStart = n.getStartPosition(); mi.srcLen = n.getLength();
        var ps = new ArrayList<String>(); for (Object p : n.parameters()) ps.add(((SingleVariableDeclaration)p).getType().toString());
        mi.signature = mi.name + "(" + String.join(",", ps) + ")";
        classes.get(qn).methods.add(mi);
        return false;
    }
}
