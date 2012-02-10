package org.jetbrains.k2js.config;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.intellij.openapi.project.Project;
import core.Dummy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Base class reprenting a configuration of translator.
 */
public abstract class Config {


    //TODO: provide some generic way to get the files of the project
    @NotNull
    private static final List<String> LIB_FILE_NAMES = Arrays.asList(
            "/core/annotations.kt",
            "/jquery/common.kt",
            "/core/javautil.kt",
            "/core/javalang.kt",
            "/core/core.kt",
            "/core/math.kt",
            "/core/json.kt",
            "/raphael/raphael.kt",
            "/html5/core.kt"
    );


    @NotNull
    private static List<JetFile> initLibFiles(@NotNull Project project) {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        for (String libFileName : LIB_FILE_NAMES) {
            InputStream stream = Dummy.class.getResourceAsStream(libFileName);
            //noinspection IOResourceOpenedButNotSafelyClosed
            JetFile file = null;
            try {
                String text = readString(stream);
                file = JetFileUtils.createPsiFile(libFileName, text, project);
            } catch (IOException e) {
                e.printStackTrace();
            }
            libFiles.add(file);
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

    static String readString(InputStream is) throws IOException {
        char[] buf = new char[2048];
        Reader r = new InputStreamReader(is, "UTF-8");
        StringBuilder s = new StringBuilder();
        while (true) {
            int n = r.read(buf);
            if (n < 0)
                break;
            s.append(buf, 0, n);
        }
        return s.toString();
    }


}
