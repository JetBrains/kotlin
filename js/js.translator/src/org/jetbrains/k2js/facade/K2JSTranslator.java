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

package org.jetbrains.k2js.facade;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.util.TextOutputImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.js.compiler.JsSourceGenerationVisitor;
import org.jetbrains.js.compiler.SourceMapBuilder;
import org.jetbrains.js.compiler.sourcemap.SourceMap3Builder;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.facade.exceptions.TranslationException;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.facade.FacadeUtils.parseString;

/**
 * An entry point of translator.
 */
public final class K2JSTranslator {

    public static final String FLUSH_SYSTEM_OUT = "Kotlin.System.flush();\n";
    public static final String GET_SYSTEM_OUT = "Kotlin.System.output();\n";

    public static void translateWithMainCallParametersAndSaveToFile(@NotNull MainCallParameters mainCall,
            @NotNull List<JetFile> files,
            @NotNull String outputPath,
            @NotNull Config config) throws TranslationException, IOException {
        K2JSTranslator translator = new K2JSTranslator(config);
        File outFile = new File(outputPath);
        TextOutputImpl output = new TextOutputImpl();
        SourceMapBuilder sourceMapBuilder = config.isSourcemap() ? new SourceMap3Builder(outFile, output, new SourceMapBuilderConsumer()) : null;
        String programCode = translator.generateProgramCode(files, mainCall, output, sourceMapBuilder);
        FileUtil.writeToFile(outFile, programCode);
        if (sourceMapBuilder != null) {
            FileUtil.writeToFile(sourceMapBuilder.getOutFile(), sourceMapBuilder.build());
        }
    }

    @NotNull
    private final Config config;


    public K2JSTranslator(@NotNull Config config) {
        this.config = config;
    }

    //NOTE: web demo related method
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public String translateStringWithCallToMain(@NotNull String programText, @NotNull String argumentsString) throws TranslationException {
        JetFile file = JetFileUtils.createJetFile("test", programText, getProject());
        String programCode = generateProgramCode(file, MainCallParameters.mainWithArguments(parseString(argumentsString))) + "\n";
        return FLUSH_SYSTEM_OUT + programCode + GET_SYSTEM_OUT;
    }

    @NotNull
    public String generateProgramCode(@NotNull JetFile file, @NotNull MainCallParameters mainCallParameters) throws TranslationException {
        return generateProgramCode(Collections.singletonList(file), mainCallParameters);
    }

    @NotNull
    public String generateProgramCode(@NotNull List<JetFile> files, @NotNull MainCallParameters mainCallParameters)
            throws TranslationException {
        return generateProgramCode(files, mainCallParameters, new TextOutputImpl(), null);
    }

    @NotNull
    public String generateProgramCode(
            @NotNull List<JetFile> files,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull TextOutputImpl output,
            @Nullable SourceMapBuilder sourceMapBuilder
    ) throws TranslationException {
        JsProgram program = generateProgram(files, mainCallParameters);
        JsSourceGenerationVisitor sourceGenerator = new JsSourceGenerationVisitor(output, sourceMapBuilder);
        program.accept(sourceGenerator);
        return output.toString();
    }

    @NotNull
    public JsProgram generateProgram(@NotNull List<JetFile> filesToTranslate,
            @NotNull MainCallParameters mainCallParameters)
            throws TranslationException {
        BindingContext bindingContext = AnalyzerFacadeForJS.analyzeFilesAndCheckErrors(filesToTranslate, config);
        return Translation.generateAst(bindingContext, filesToTranslate, mainCallParameters, config);
    }

    @NotNull
    private Project getProject() {
        return config.getProject();
    }
}