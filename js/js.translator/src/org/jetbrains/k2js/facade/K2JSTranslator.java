/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.plugin.JetMainDetector;
import org.jetbrains.k2js.analyze.Analyzer;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.IDEAConfig;
import org.jetbrains.k2js.config.TestConfig;
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.utils.GenerationUtils;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getNamespaceName;
import static org.jetbrains.k2js.utils.JetFileUtils.createPsiFileList;

//TODO: clean up the code

/**
 * @author Pavel Talanov
 *         <p/>
 *         An entry point of translator.
 */
public final class K2JSTranslator {

    public static void translateWithCallToMainAndSaveToFile(@NotNull List<JetFile> files,
                                                            @NotNull String outputPath,
                                                            @NotNull Project project) throws Exception {
        K2JSTranslator translator = new K2JSTranslator(new IDEAConfig(project));
        String programCode = translator.generateProgramCode(files) + "\n";
        JetFile fileWithMain = JetMainDetector.getFileWithMain(files);
        if (fileWithMain == null) {
            throw new RuntimeException("No file with main detected.");
        }
        String callToMain = translator.generateCallToMain(fileWithMain, "");
        FileWriter writer = new FileWriter(new File(outputPath));
        writer.write(programCode + callToMain);
        writer.close();
    }

    public static void saveProgramToFile(@NotNull String outputFile, @NotNull JsProgram program) throws IOException {
        CodeGenerator generator = new CodeGenerator();
        generator.generateToFile(program, new File(outputFile));
    }

    @NotNull
    private Config config;


    public K2JSTranslator(@NotNull Config config) {
        this.config = config;
    }

    //TODO: refactor
    @NotNull
    public String translateStringWithCallToMain(@NotNull String programText, @NotNull String argumentsString) {
        JetFile file = JetFileUtils.createPsiFile("test", programText, getProject());
        String programCode = generateProgramCode(file) + "\n";
        String flushOutput = "Kotlin.System.flush();\n";
        String callToMain = generateCallToMain(file, argumentsString);
        String programOutput = "Kotlin.System.output();\n";
        return programCode + flushOutput + callToMain + programOutput;
    }

    @NotNull
    private String generateProgramCode(@NotNull JetFile psiFile) {
        JsProgram program = generateProgram(Arrays.asList(psiFile));
        CodeGenerator generator = new CodeGenerator();
        return generator.generateToString(program);
    }

    @NotNull
    private String generateProgramCode(@NotNull List<JetFile> files) {
        JsProgram program = generateProgram(files);
        CodeGenerator generator = new CodeGenerator();
        return generator.generateToString(program);
    }

    @NotNull
    public BindingContext analyzeProgramCode(@NotNull String programText) {
        JetFile file = JetFileUtils.createPsiFile("test", programText, getProject());
        return Analyzer.analyzeFiles(Arrays.asList(file), config);
    }


    @NotNull
    public JsProgram generateProgram(@NotNull List<JetFile> filesToTranslate) {
        JetStandardLibrary.initialize(config.getProject());
        BindingContext bindingContext = Analyzer.analyzeFilesAndCheckErrors(filesToTranslate, config);
        return Translation.generateAst(bindingContext, Analyzer.withJsLibAdded(filesToTranslate, config));
    }


    @NotNull
    public String generateCallToMain(@NotNull JetFile file, @NotNull String argumentString) {
        String namespaceName = getNamespaceName(file);
        List<String> arguments = parseString(argumentString);
        return GenerationUtils.generateCallToMain(namespaceName, arguments);
    }

    @NotNull
    private List<String> parseString(@NotNull String argumentString) {
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
