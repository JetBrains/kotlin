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
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.K2JSTranslator;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.config.TestConfigFactory;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.List;

//TODO: use method object
public final class TranslationUtils {

    private TranslationUtils() {
    }

    @NotNull
    private static SoftReference<BindingContext> cachedLibraryContext = new SoftReference<BindingContext>(null);

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
    public static BindingContext getLibraryContext(@NotNull Project project) {
        BindingContext context = cachedLibraryContext.get();
        if (context == null) {
            List<JetFile> allLibFiles = getAllLibFiles(project);
            Predicate<PsiFile> filesWithCode = new Predicate<PsiFile>() {
                @Override
                public boolean apply(@javax.annotation.Nullable PsiFile file) {
                    assert file != null;
                    return isFileWithCode((JetFile) file);
                }
            };
            AnalyzeExhaust exhaust = AnalyzerFacadeForJS
                    .analyzeFiles(allLibFiles, filesWithCode, Config.getEmptyConfig(project));
            context = exhaust.getBindingContext();
            AnalyzerFacadeForJS.checkForErrors(allLibFiles, context);
            cachedLibraryContext = new SoftReference<BindingContext>(context);
        }
        return context;
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
        BindingContext preanalyzedContext = getLibraryContext(project);
        return configFactory.create(project, version, getLibFilesWithCode(getAllLibFiles(project)), preanalyzedContext);
    }

    public static void translateFiles(@NotNull Project project, @NotNull List<String> inputFiles,
            @NotNull String outputFile,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull EcmaVersion version, TestConfigFactory configFactory) throws Exception {
        List<JetFile> jetFiles = createJetFileList(project, inputFiles, null);
        K2JSTranslator translator = new K2JSTranslator(getConfig(project, version, configFactory));
        FileUtil.writeToFile(new File(outputFile), translator.generateProgramCode(jetFiles, mainCallParameters));
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

    private static List<JetFile> createJetFileList(@NotNull Project project, @NotNull List<String> list, @Nullable String root) {
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
