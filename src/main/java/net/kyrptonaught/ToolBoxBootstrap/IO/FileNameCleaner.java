package net.kyrptonaught.ToolBoxBootstrap.IO;

import java.nio.file.Path;
import java.util.Arrays;

public class FileNameCleaner {
    final static int[] illegalChars = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124};

    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        int len = badFileName.codePointCount(0, badFileName.length());
        for (int i = 0; i < len; i++) {
            int c = badFileName.codePointAt(i);
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.appendCodePoint(c);
            }
        }
        return cleanName.toString();
    }

    public static String removeFirstSlashAndClean(String badFileName) {
        if (badFileName.startsWith("/"))
            badFileName = badFileName.substring(1);
        return cleanFileName(badFileName);
    }

    public static String fixPathSeparator(String name) {
        return name.replaceAll("\\\\", "/");
    }

    public static String fixPathSeparator(Path path) {
        return fixPathSeparator(path.toString());
    }

    public static String pathToString(Path path) {
        String name = fixPathSeparator(path.toString());


        if (name.startsWith("installs/")) {
            int nextSlash = name.indexOf("/", 9);
            if (nextSlash == -1) return "";

            return name.substring(nextSlash + 1);
        }
        return name;
    }
}