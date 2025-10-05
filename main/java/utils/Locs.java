package utils;

public class Locs {
    public static int countNonEmpty(String text) {
        int c=0; for (String line : text.split("\\R")) if (!line.trim().isEmpty()) c++; return c;
    }
}