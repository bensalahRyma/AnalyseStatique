package utils;

import java.util.*;
public class Args {
    public static Map<String,String> parse(String[] args){
        Map<String,String> m=new HashMap<>();
        for (int i=0;i<args.length;i++) if (args[i].startsWith("--")) {
            String k=args[i].substring(2);
            String v=(i+1<args.length && !args[i+1].startsWith("--"))?args[++i]:"true";
            m.put(k,v);
        } return m;
    }
}