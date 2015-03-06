/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.io.URLUtil;
import kotlin.Function1;
import kotlin.Function2;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.JavaScript;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.utils.LibraryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.utils.LibraryUtils.isKotlinJavascriptLibrary;
import static org.jetbrains.kotlin.utils.LibraryUtils.isKotlinJavascriptStdLibrary;

public class LibrarySourcesConfig extends Config {
    @NotNull
    public static final Key<String> EXTERNAL_MODULE_NAME = Key.create("externalModule");
    @NotNull
    public static final String UNKNOWN_EXTERNAL_MODULE_NAME = "<unknown>";

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
    public List<String> getLibraries() {
        return files;
    }

    @NotNull
    @Override
    protected List<JetFile> generateLibFiles() {
        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        final List<JetFile> jetFiles = new ArrayList<JetFile>();
        final PsiManager psiManager = PsiManager.getInstance(getProject());

        Function1<String, Unit> report = new Function1<String, Unit>() {
            @Override
            public Unit invoke(String message) {
                throw new IllegalStateException(message);
            }
        };

        Function2<String, VirtualFile, Unit> action = new Function2<String, VirtualFile, Unit>() {
            @Override
            public Unit invoke(String moduleName, VirtualFile file) {
                JetFileCollector jetFileCollector = new JetFileCollector(jetFiles, moduleName, psiManager);
                VfsUtilCore.visitChildrenRecursively(file, jetFileCollector);
                return Unit.INSTANCE$;
            }
        };

        boolean hasErrors = checkLibFilesAndReportErrors(report, action);
        assert !hasErrors : "hasErrors should be false";

        return jetFiles;
    }

    @Override
    public boolean checkLibFilesAndReportErrors(@NotNull Function1<String, Unit> report) {
        return checkLibFilesAndReportErrors(report, null);
    }

    private boolean checkLibFilesAndReportErrors(@NotNull Function1<String, Unit> report, @Nullable Function2<String, VirtualFile, Unit> action) {
        if (files.isEmpty()) {
            return false;
        }

        VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
        VirtualFileSystem jarFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL);

        String moduleName = null;

        for (String path : files) {
            VirtualFile file;
            String actualModuleName = moduleName;
            if (path.charAt(0) == '@') {
                moduleName = path.substring(1);
                continue;
            }

            File filePath = new File(path);
            if (!filePath.exists()) {
                report.invoke("Path '" + path + "'does not exist");
                return true;
            }

            if (path.endsWith(".jar") || path.endsWith(".zip")) {
                file = jarFileSystem.findFileByPath(path + URLUtil.JAR_SEPARATOR);
            }
            else {
                file = fileSystem.findFileByPath(path);
            }

            if (file == null) {
                report.invoke("File '" + path + "'does not exist or could not be read");
                return true;
            }
            else {
                if (isKotlinJavascriptStdLibrary(filePath)) {
                    actualModuleName = STDLIB_JS_MODULE_NAME;
                }
                else if (isKotlinJavascriptLibrary(filePath)) {
                    actualModuleName = LibraryUtils.getKotlinJsModuleName(filePath);
                }
                else if (actualModuleName == null) {
                    report.invoke("'" + path + "' is not a valid Kotlin Javascript library");
                    return true;
                }
                if (actualModuleName == null) {
                    report.invoke("Could not find " + LibraryUtils.KOTLIN_JS_MODULE_NAME + " for '" + path + "'");
                    return true;
                }
                if (action != null) {
                    action.invoke(actualModuleName, file);
                }
            }
            moduleName = null;
        }

        return false;
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
