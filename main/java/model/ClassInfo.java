package model;

import java.util.ArrayList;
import java.util.List;

public class ClassInfo {
    public String packageName, qualifiedName;
    public int attributeCount;
    public List<MethodInfo> methods = new ArrayList<>();
    public int srcStart, srcLen;
}
