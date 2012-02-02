package org.jetbrains.k2js.config;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public abstract class Config {

    @NotNull
    private static final String PATH_TO_JS_LIB_SRC = getPathToJsLibSrc();

    //TODO: provide some generic way to access
    @NotNull
    private static final List<String> LIB_FILE_NAMES = Arrays.asList(
            PATH_TO_JS_LIB_SRC + "\\core\\annotations.kt",
            PATH_TO_JS_LIB_SRC + "\\jquery\\common.kt",
            PATH_TO_JS_LIB_SRC + "\\core\\javautil.kt",
            PATH_TO_JS_LIB_SRC + "\\core\\javalang.kt",
            PATH_TO_JS_LIB_SRC + "\\core\\core.kt",
            PATH_TO_JS_LIB_SRC + "\\raphael\\raphael.kt",
            PATH_TO_JS_LIB_SRC + "\\core\\json.kt",
            PATH_TO_JS_LIB_SRC + "\\html5\\core.kt",
            PATH_TO_JS_LIB_SRC + "\\core\\javautilcollections.kt"
    );


    @NotNull
    private static String getPathToJsLibSrc() {
        try {
            File file = new File("config.txt");
            List<String> lines = Files.readLines(file, Charsets.UTF_8);
            return lines.get(0);
        } catch (Exception ex) {
            return "jslib\\src";
        }
    }

    @NotNull
    private static List<JetFile> initLibFiles(@NotNull Project project) {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        for (String libFileName : LIB_FILE_NAMES) {
            libFiles.add(JetFileUtils.loadPsiFile(libFileName, project));
        }
        return libFiles;
    }


    @Nullable
    private /*var*/ List<JetFile> jsLibFiles = null;

    @NotNull
    public abstract Project getProject();

    @NotNull
    public List<JetFile> getLibFiles() {
        if (jsLibFiles == null) {
            jsLibFiles = initLibFiles(getProject());
        }

        return jsLibFiles;
    }
}
