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

package org.jetbrains.k2js.test.utils;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.facade.exceptions.TranslationException;
import org.jetbrains.k2js.test.config.TestConfigFactory;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.List;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.k2js.facade.K2JSTranslator.translateWithMainCallParameters;

//TODO: use method object
public final class TranslationUtils {

    private TranslationUtils() {
    }

    @NotNull
    private static SoftReference<AnalyzeExhaust> cachedLibraryExhaust = new SoftReference<AnalyzeExhaust>(null);

    @Nullable
    private static List<JetFile> libFiles = null;

    @NotNull
    private static List<JetFile> getAllLibFiles(@NotNull Project project) {
        if (libFiles == null) {
            libFiles = initLibFiles(project);
        }
        return libFiles;
    }

    @NotNull
    public static AnalyzeExhaust getLibraryAnalyzeExhaust(@NotNull Project project) {
        AnalyzeExhaust cachedModule = cachedLibraryExhaust.get();
        if (cachedModule != null) {
            return cachedModule;
        }
        List<JetFile> allLibFiles = getAllLibFiles(project);
        Predicate<PsiFile> filesWithCode = new Predicate<PsiFile>() {
            @Override
            public boolean apply(@Nullable PsiFile file) {
                assert file != null;
                return isFileWithCode((JetFile) file);
            }
        };
        AnalyzeExhaust exhaust = TopDownAnalyzerFacadeForJS.analyzeFiles(allLibFiles, filesWithCode, Config.getEmptyConfig(project));
        TopDownAnalyzerFacadeForJS.checkForErrors(allLibFiles, exhaust.getBindingContext());
        cachedLibraryExhaust = new SoftReference<AnalyzeExhaust>(exhaust);
        return exhaust;
    }

    private static boolean isFileWithCode(@NotNull JetFile file) {
        for (String filename : Config.LIB_FILES_WITH_CODE) {
            if (file.getName().contains(filename)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static Config getConfig(@NotNull Project project, @NotNull EcmaVersion version, @NotNull TestConfigFactory configFactory) {
        AnalyzeExhaust libraryAnalyzeExhaust = getLibraryAnalyzeExhaust(project);
        return configFactory.create(
                project, version, getLibFilesWithCode(getAllLibFiles(project)),
                libraryAnalyzeExhaust.getBindingContext(), libraryAnalyzeExhaust.getModuleDescriptor());
    }

    public static void translateFiles(
            @NotNull MainCallParameters mainCall,
            @NotNull List<JetFile> files,
            @NotNull File outputFile,
            @Nullable File outputPrefixFile,
            @Nullable File outputPostfixFile,
            @NotNull Config config,
            @NotNull Consumer<JsNode> astConsumer
    ) throws TranslationException, IOException {
        OutputFileCollection outputFiles =
                translateWithMainCallParameters(mainCall, files, outputFile, outputPrefixFile, outputPostfixFile, config, astConsumer);
        OutputUtilsPackage.writeAllTo(outputFiles, outputFile.getParentFile());
    }

    public static JsNode translateFilesAndGetAst(
            @NotNull MainCallParameters mainCall,
            @NotNull List<JetFile> files,
            @NotNull File outputFile,
            @Nullable File outputPrefixFile,
            @Nullable File outputPostfixFile,
            @NotNull Config config
    ) throws TranslationException, IOException {
        final Ref<JsNode> ref = new Ref<JsNode>();

        translateFiles(mainCall, files, outputFile, outputPrefixFile, outputPostfixFile, config, new Consumer<JsNode>() {
            @Override
            public void consume(JsNode node) {
                ref.set(node);
            }
        });

        return ref.get();
    }

    @NotNull
    private static List<JetFile> initLibFiles(@NotNull Project project) {
        return createJetFileList(project, LibraryFilePathsUtil.getBasicLibraryFiles(), null);
    }

    @NotNull
    private static List<JetFile> getLibFilesWithCode(@NotNull List<JetFile> allFiles) {
        List<JetFile> result = Lists.newArrayList();
        for (JetFile file : allFiles) {
            if (isFileWithCode(file)) {
                result.add(file);
            }
        }
        return result;
    }

    public static List<JetFile> createJetFileList(@NotNull Project project, @NotNull List<String> list, @Nullable String root) {
        List<JetFile> libFiles = Lists.newArrayList();
        for (String libFileName : list) {
            try {
                String path = root == null ? libFileName : (root + libFileName);
                String text = FileUtil.loadFile(new File(path), true);
                JetFile jetFile = JetPsiFactory(project).createFile(path, text);
                libFiles.add(jetFile);
            }
            catch (IOException e) {
                //TODO: throw generic exception
                throw new IllegalStateException(e);
            }
        }
        return libFiles;

    }
}
