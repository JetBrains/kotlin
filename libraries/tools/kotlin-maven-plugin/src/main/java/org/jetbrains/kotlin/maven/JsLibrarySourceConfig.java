package org.jetbrains.kotlin.maven;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.internal.com.intellij.openapi.project.Project;
import org.jetbrains.jet.internal.com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A Config implementation which is configured with a directory to find the standard library names from
 */
public class JsLibrarySourceConfig extends Config {

    @Nullable
    private /*var*/ List<JetFile> jsLibFiles = null;
    @NotNull
    private String librarySourceDir;

    public JsLibrarySourceConfig(@NotNull Project project, @NotNull EcmaVersion version, @NotNull String librarySourceDir) {
        super(project, version);
        this.librarySourceDir = librarySourceDir;
    }

    @NotNull
    private List<JetFile> initLibFiles(@NotNull Project project) {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        for (String libFileName : LIB_FILE_NAMES) {
            JetFile file = null;
            try {
                @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
                InputStream stream = new FileInputStream(librarySourceDir + libFileName);
                try {
                    String text = FileUtil.loadTextAndClose(stream);
                    file = JetFileUtils.createPsiFile(libFileName, text, project);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                libFiles.add(file);
            }
            catch (Exception e) {
                //TODO: throw generic exception
                throw new IllegalStateException(e);
            }
        }
        return libFiles;
    }

    @Override
    @NotNull
    public List<JetFile> generateLibFiles() {
        if (jsLibFiles == null) {
            jsLibFiles = initLibFiles(getProject());
        }
        return jsLibFiles;
    }
}
