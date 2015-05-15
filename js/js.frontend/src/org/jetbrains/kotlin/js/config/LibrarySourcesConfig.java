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
import com.intellij.util.PathUtil;
import com.intellij.util.io.URLUtil;
import kotlin.Function1;
import kotlin.Function2;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.js.JavaScript;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils;
import org.jetbrains.kotlin.utils.LibraryUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.utils.LibraryUtils.isKotlinJavascriptLibraryWithMetadata;
import static org.jetbrains.kotlin.utils.LibraryUtils.isOldKotlinJavascriptLibrary;
import static org.jetbrains.kotlin.utils.PathUtil.getKotlinPathsForDistDirectory;

public class LibrarySourcesConfig extends Config {
    public static final List<String> JS_STDLIB =
            Collections.singletonList(getKotlinPathsForDistDirectory().getJsStdLibJarPath().getAbsolutePath());

    @NotNull
    public static final Key<String> EXTERNAL_MODULE_NAME = Key.create("externalModule");
    @NotNull
    public static final String UNKNOWN_EXTERNAL_MODULE_NAME = "<unknown>";

    public static final String STDLIB_JS_MODULE_NAME = "stdlib";
    public static final String BUILTINS_JS_MODULE_NAME = "builtins";
    public static final String BUILTINS_JS_FILE_NAME = BUILTINS_JS_MODULE_NAME + JavaScript.DOT_EXTENSION;
    public static final String STDLIB_JS_FILE_NAME = STDLIB_JS_MODULE_NAME + JavaScript.DOT_EXTENSION;

    private final boolean isUnitTestConfig;

    @NotNull
    private final List<String> files;

    private LibrarySourcesConfig(
            @NotNull Project project,
            @NotNull String moduleId,
            @NotNull List<String> files,
            @NotNull EcmaVersion ecmaVersion,
            boolean sourceMap,
            boolean inlineEnabled,
            boolean isUnitTestConfig,
            boolean metaInfo
    ) {
        super(project, moduleId, ecmaVersion, sourceMap, inlineEnabled, metaInfo);
        this.files = files;
        this.isUnitTestConfig = isUnitTestConfig;
    }

    @Override
    public boolean isTestConfig() {
        return isUnitTestConfig;
    }

    @NotNull
    public List<String> getLibraries() {
        return files;
    }

    @Override
    protected void init(@NotNull final List<JetFile> sourceFilesInLibraries, @NotNull final List<KotlinJavascriptMetadata> metadata) {
        if (files.isEmpty()) return;

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
                if (moduleName != null) {
                    JetFileCollector jetFileCollector = new JetFileCollector(sourceFilesInLibraries, moduleName, psiManager);
                    VfsUtilCore.visitChildrenRecursively(file, jetFileCollector);
                }
                else {
                    String libraryPath = PathUtil.getLocalPath(file);
                    assert libraryPath != null : "libraryPath for " + file + " should not be null";
                    metadata.addAll(KotlinJavascriptMetadataUtils.loadMetadata(libraryPath));
                }

                return Unit.INSTANCE$;
            }
        };

        boolean hasErrors = checkLibFilesAndReportErrors(report, action);
        assert !hasErrors : "hasErrors should be false";
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

        for (String path : files) {
            VirtualFile file;

            File filePath = new File(path);
            if (!filePath.exists()) {
                report.invoke("Path '" + path + "' does not exist");
                return true;
            }

            if (path.endsWith(".jar") || path.endsWith(".zip")) {
                file = jarFileSystem.findFileByPath(path + URLUtil.JAR_SEPARATOR);
            }
            else {
                file = fileSystem.findFileByPath(path);
            }

            if (file == null) {
                report.invoke("File '" + path + "' does not exist or could not be read");
                return true;
            }

            String moduleName;

            if (isOldKotlinJavascriptLibrary(filePath)) {
                moduleName = LibraryUtils.getKotlinJsModuleName(filePath);
            }
            else if (isKotlinJavascriptLibraryWithMetadata(filePath)) {
                moduleName = null;
            }
            else {
                report.invoke("'" + path + "' is not a valid Kotlin Javascript library");
                return true;
            }

            if (action != null) {
                action.invoke(moduleName, file);
            }
        }

        return false;
    }

    public static class Builder {
        Project project;
        String moduleId;
        List<String> files;
        @NotNull
        EcmaVersion ecmaVersion = EcmaVersion.defaultVersion();
        boolean sourceMap = false;
        boolean inlineEnabled = true;
        boolean isUnitTestConfig = false;
        boolean metaInfo = false;

        public Builder(@NotNull Project project, @NotNull String moduleId, @NotNull List<String> files) {
            this.project = project;
            this.moduleId = moduleId;
            this.files = files;
        }

        public Builder ecmaVersion(@NotNull EcmaVersion ecmaVersion) {
            this.ecmaVersion = ecmaVersion;
            return this;
        }

        public Builder sourceMap(boolean sourceMap) {
            this.sourceMap = sourceMap;
            return this;
        }

        public Builder inlineEnabled(boolean inlineEnabled) {
            this.inlineEnabled = inlineEnabled;
            return this;
        }

        public Builder isUnitTestConfig(boolean isUnitTestConfig) {
            this.isUnitTestConfig = isUnitTestConfig;
            return this;
        }

        public Builder metaInfo(boolean metaInfo) {
            this.metaInfo = metaInfo;
            return this;
        }

        public Config build() {
            return new LibrarySourcesConfig(project, moduleId, files, ecmaVersion, sourceMap, inlineEnabled, isUnitTestConfig, metaInfo);
        }
    }

    protected static JetFile getJetFileByVirtualFile(VirtualFile file, String moduleName, PsiManager psiManager) {
        PsiFile psiFile = psiManager.findFile(file);
        assert psiFile != null;

        setupPsiFile(psiFile, moduleName);
        return (JetFile) psiFile;
    }

    protected static void setupPsiFile(PsiFile psiFile, String moduleName) {
        psiFile.putUserData(EXTERNAL_MODULE_NAME, moduleName);
    }

    private static class JetFileCollector extends VirtualFileVisitor {
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
