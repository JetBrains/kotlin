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

package org.jetbrains.k2js.facade;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.facade.exceptions.TranslationException;
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Pavel Talanov
 *         <p/>
 *         An entry point of translator.
 */
public final class K2JSTranslator {

    public static void translateWithCallToMainAndSaveToFile(@NotNull List<JetFile> files,
            @NotNull String outputPath,
            @NotNull Config config) throws Exception {
        K2JSTranslator translator = new K2JSTranslator(config);
        String programCode = translator.generateProgramCode(files, MainCallParameters.mainWithoutArguments()) + "\n";
        writeCodeToFile(outputPath, programCode);
    }

    private static void writeCodeToFile(@NotNull String outputPath, @NotNull String programCode) throws IOException {
        File file = new File(outputPath);
        FileUtil.createParentDirs(file);
        FileWriter writer = new FileWriter(file);
        try {
            writer.write(programCode);
        }
        finally {
            writer.close();
        }
    }

    @NotNull
    private final Config config;


    public K2JSTranslator(@NotNull Config config) {
        this.config = config;
    }

    //TODO: web demo related method
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public String translateStringWithCallToMain(@NotNull String programText, @NotNull String argumentsString) throws TranslationException {
        JetFile file = JetFileUtils.createPsiFile("test", programText, getProject());
        String programCode = generateProgramCode(file, MainCallParameters.mainWithArguments(parseString(argumentsString))) + "\n";
        String flushOutput = "Kotlin.System.flush();\n";
        String programOutput = "Kotlin.System.output();\n";
        return flushOutput + programCode + programOutput;
    }

    @NotNull
    public String generateProgramCode(@NotNull JetFile file, @NotNull MainCallParameters mainCallParameters) throws TranslationException {
        JsProgram program = generateProgram(Arrays.asList(file), mainCallParameters);
        CodeGenerator generator = new CodeGenerator();
        return generator.generateToString(program);
    }

    @NotNull
    public String generateProgramCode(@NotNull List<JetFile> files, @NotNull MainCallParameters mainCallParameters)
            throws TranslationException {
        JsProgram program = generateProgram(files, mainCallParameters);
        CodeGenerator generator = new CodeGenerator();
        return generator.generateToString(program);
    }

    @NotNull
    public JsProgram generateProgram(@NotNull List<JetFile> filesToTranslate, @NotNull MainCallParameters mainCallParameters)
            throws TranslationException {
        JetStandardLibrary.initialize(config.getProject());
        BindingContext bindingContext = AnalyzerFacadeForJS.analyzeFilesAndCheckErrors(filesToTranslate, config);
        Collection<JetFile> files = AnalyzerFacadeForJS.withJsLibAdded(filesToTranslate, config);

        return Translation.generateAst(bindingContext, Lists.newArrayList(files), mainCallParameters, config.getTarget());
    }

    //TODO: util
    @NotNull
    private static List<String> parseString(@NotNull String argumentString) {
        List<String> result = new ArrayList<String>();
        StringTokenizer stringTokenizer = new StringTokenizer(argumentString);
        while (stringTokenizer.hasMoreTokens()) {
            result.add(stringTokenizer.nextToken());
        }
        return result;
    }

    @NotNull
    private Project getProject() {
        return config.getProject();
    }
}
