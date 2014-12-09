/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.k2js.JavaScript;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.utils.LibraryUtils.isJsRuntimeLibrary;

public class LibrarySourcesConfig extends Config {
    @NotNull
    public static final Key<String> EXTERNAL_MODULE_NAME = Key.create("externalModule");
    @NotNull
    public static final String UNKNOWN_EXTERNAL_MODULE_NAME = "<unknown>";

    @NotNull
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.k2js.config.LibrarySourcesConfig");
    public static final String STDLIB_JS_MODULE_NAME = "stdlib";
    public static final String BUILTINS_JS_MODULE_NAME = "builtins";
    public static final String BUILTINS_JS_FILE_NAME = BUILTINS_JS_MODULE_NAME + JavaScript.DOT_EXTENSION;
    public static final String STDLIB_JS_FILE_NAME = STDLIB_JS_MODULE_NAME + JavaScript.DOT_EXTENSION;

    @NotNull
    private final List<String> files;

    public LibrarySourcesConfig(
            @NotNull Project project,
            @NotNull String moduleId,
            @NotNull List<String> files,
            @NotNull EcmaVersion ecmaVersion,
            boolean sourcemap,
            boolean inlineEnabled
    ) {
        super(project, moduleId, ecmaVersion, sourcemap, inlineEnabled);
        this.files = files;
    }

    @NotNull
    @Override
    protected List<JetFile> generateLibFiles() {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        List<JetFile> jetFiles = new ArrayList<JetFile>();
        String moduleName = UNKNOWN_EXTERNAL_MODULE_NAME;
        VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
        VirtualFileSystem jarFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL);

        PsiManager psiManager = PsiManager.getInstance(getProject());

        for (String path : files) {
            if (path.charAt(0) == '@') {
                moduleName = path.substring(1);
                continue;
            }

            VirtualFile file;
            String actualModuleName = moduleName;

            if (path.endsWith(".jar") || path.endsWith(".zip")) {
                file = jarFileSystem.findFileByPath(path + URLUtil.JAR_SEPARATOR);

                if (isJsRuntimeLibrary(new File(path))) {
                    actualModuleName = STDLIB_JS_MODULE_NAME;
                }
            }
            else {
                file = fileSystem.findFileByPath(path);
            }

            if (file == null) {
                LOG.error("File '" + path + "' not found.'");
            }
            else {
                JetFileCollector jetFileCollector = new JetFileCollector(jetFiles, actualModuleName, psiManager);
                VfsUtilCore.visitChildrenRecursively(file, jetFileCollector);
            }
        }

        return jetFiles;
    }

    protected JetFile getJetFileByVirtualFile(VirtualFile file, String moduleName, PsiManager psiManager) {
        PsiFile psiFile = psiManager.findFile(file);
        assert psiFile != null;

        setupPsiFile(psiFile, moduleName);
        return (JetFile) psiFile;
    }

    protected static void setupPsiFile(PsiFile psiFile, String moduleName) {
        psiFile.putUserData(EXTERNAL_MODULE_NAME, moduleName);
    }

    private class JetFileCollector extends VirtualFileVisitor {
        private final List<JetFile> jetFiles;
        private final String moduleName;
        private final PsiManager psiManager;

        private JetFileCollector(List<JetFile> files, String name, PsiManager manager) {
            moduleName = name;
            psiManager = manager;
            jetFiles = files;
        }

        @Override
        public boolean visitFile(@NotNull VirtualFile file) {
            if (!file.isDirectory() && StringUtil.notNullize(file.getExtension()).equalsIgnoreCase(JetFileType.EXTENSION)) {
                jetFiles.add(getJetFileByVirtualFile(file, moduleName, psiManager));
            }
            return true;
        }
    }
}
