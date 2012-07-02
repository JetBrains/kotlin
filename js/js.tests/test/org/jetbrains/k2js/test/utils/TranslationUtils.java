/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import closurecompiler.internal.com.google.common.collect.Maps;
import com.google.common.base.Predicates;
import com.google.dart.compiler.backend.js.ast.JsProgram;
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
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.test.config.TestConfig;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.utils.JetFileUtils.createPsiFileList;

/**
 * @author Pavel Talanov
 */
public final class TranslationUtils {

    private TranslationUtils() {
    }

    @NotNull
    private static final Map<EcmaVersion, Config> testConfigs = Maps.newHashMap();

    @Nullable
    private static BindingContext libraryContext = null;

    @NotNull
    public static BindingContext getLibraryContext(@NotNull Project project, @NotNull List<JetFile> files) {
        if (libraryContext == null) {
            AnalyzeExhaust exhaust = AnalyzerFacadeForJS
                    .analyzeFiles(files, Predicates.<PsiFile>alwaysFalse(),
                                  Config.getEmptyConfig(project));
            libraryContext = exhaust.getBindingContext();
            AnalyzerFacadeForJS.checkForErrors(files, libraryContext);
        }
        return libraryContext;
    }

    @NotNull
    public static Config getConfig(@NotNull Project project, @NotNull EcmaVersion version) {
        Config config = testConfigs.get(version);
        if (config == null) {
            List<JetFile> files = initLibFiles(project);
            config = new TestConfig(project, version, files, getLibraryContext(project, files));
            testConfigs.put(version, config);
        }
        return config;
    }

    public static void translateFiles(@NotNull Project project, @NotNull List<String> inputFiles,
            @NotNull String outputFile,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull EcmaVersion version,
            @Nullable List<String> rawStatements) throws Exception {
        List<JetFile> psiFiles = createPsiFileList(inputFiles, project);
        JsProgram program = getTranslator(project, version).generateProgram(psiFiles, mainCallParameters, rawStatements);
        FileWriter writer = new FileWriter(new File(outputFile));
        try {
            writer.write(CodeGenerator.generateProgramToString(program, null));
        }
        finally {
            writer.close();
        }
    }

    @NotNull
    private static K2JSTranslator getTranslator(@NotNull Project project, @NotNull EcmaVersion version) {
        return new K2JSTranslator(getConfig(project, version));
    }

    @NotNull
    public static List<JetFile> initLibFiles(@NotNull Project project) {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        for (String libFileName : Config.LIB_FILE_NAMES) {
            JetFile file = null;
            try {
                @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
                InputStream stream = new FileInputStream(Config.LIBRARIES_LOCATION + libFileName);
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
}
