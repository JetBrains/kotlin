package org.jetbrains.k2js.config;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public abstract class Config {

    //TODO: provide some generic way to access
    private static final List<String> LIB_FILE_NAMES = Arrays.asList(
            "C:\\Dev\\Projects\\jet-contrib\\k2js\\jslib\\src\\core\\annotations.kt",
            "C:\\Dev\\Projects\\jet-contrib\\k2js\\jslib\\src\\jquery\\common.kt",
            "C:\\Dev\\Projects\\jet-contrib\\k2js\\jslib\\src\\core\\javautil.kt",
            "C:\\Dev\\Projects\\jet-contrib\\k2js\\jslib\\src\\core\\core.kt"
    );

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
