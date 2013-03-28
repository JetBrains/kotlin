package org.jetbrains.jet.di;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;

import java.io.File;
import java.io.IOException;

public class GeneratorsFileUtil {
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void writeFileIfContentChanged(File file, String newText) throws IOException {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            if (parentFile.mkdirs()) {
                System.out.println("Directory created: " + parentFile.getAbsolutePath());
            }
            else {
                throw new IllegalStateException("Cannot create directory: " + parentFile);
            }
        }

        if (checkFileIgnoringLineSeparators(file, newText)) {
            System.out.println("Not changed: " + file.getAbsolutePath());
            return;
        }

        boolean useTmpfile = !SystemInfo.isWindows;

        File tmpfile = useTmpfile ? new File(file.getName() + ".tmp") : file;

        FileUtil.writeToFile(tmpfile, newText);
        System.out.println("File written: " + tmpfile.getAbsolutePath());
        if (useTmpfile) {
            if (!tmpfile.renameTo(file)) {
                throw new RuntimeException("failed to rename " + tmpfile + " to " + file);
            }
            System.out.println("Renamed " + tmpfile + " to " + file);
        }
        System.out.println();
    }

    private static boolean checkFileIgnoringLineSeparators(File file, String content) {
        String currentContent;
        try {
            currentContent = FileUtil.loadFile(file, true);
        }
        catch (Throwable ignored) {
            return false;
        }

        return StringUtil.convertLineSeparators(content).equals(currentContent);
    }
}
