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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.facade.exceptions.TranslationException;
import org.jetbrains.k2js.test.config.TestConfigFactory;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.List;

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
        AnalyzeExhaust exhaust = AnalyzerFacadeForJS.analyzeFiles(allLibFiles, filesWithCode, Config.getEmptyConfig(project));
        AnalyzerFacadeForJS.checkForErrors(allLibFiles, exhaust.getBindingContext());
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
            @NotNull Project project,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull List<String> inputFiles,
            @NotNull String outputFile,
            @NotNull EcmaVersion version,
            @NotNull TestConfigFactory configFactory
    ) throws Exception {
        List<JetFile> jetFiles = createJetFileList(project, inputFiles, null);
        Config config = getConfig(project, version, configFactory);
        translateFiles(mainCallParameters, jetFiles, outputFile, null, null, config);
    }

    public static void translateFiles(
            @NotNull MainCallParameters mainCall,
            @NotNull List<JetFile> files,
            @NotNull String outputPath,
            @Nullable File outputPrefixFile,
            @Nullable File outputPostfixFile,
            @NotNull Config config
    ) throws TranslationException, IOException {
        File outputFile = new File(outputPath);
        OutputFileCollection outputFiles = translateWithMainCallParameters(mainCall, files, outputFile, outputPrefixFile, outputPostfixFile, config);
        OutputUtilsPackage.writeAllTo(outputFiles, outputFile.getParentFile());
    }

    @NotNull
    private static List<JetFile> initLibFiles(@NotNull Project project) {
        return createJetFileList(project, Config.LIB_FILE_NAMES, Config.LIBRARIES_LOCATION);
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
                String text = FileUtil.loadFile(new File(path));
                JetFile jetFile = JetFileUtils.createJetFile(path, text, project);
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
