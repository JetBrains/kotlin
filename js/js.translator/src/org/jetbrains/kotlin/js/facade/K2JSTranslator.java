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

package org.jetbrains.kotlin.js.facade;

import com.google.common.base.Predicates;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.util.TextOutputImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalysisResult;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.utils.fileUtils.FileUtilsPackage;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFile;
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException;
import org.jetbrains.kotlin.js.inline.JsInliner;
import org.jetbrains.kotlin.js.sourceMap.JsSourceGenerationVisitor;
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder;
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilder;
import org.jetbrains.kotlin.js.translate.general.Translation;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.DiagnosticUtils.hasError;
import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.kotlin.js.facade.FacadeUtils.parseString;

/**
 * An entry point of translator.
 */
public final class K2JSTranslator {

    public static final String FLUSH_SYSTEM_OUT = "Kotlin.out.flush();\n";
    public static final String GET_SYSTEM_OUT = "Kotlin.out.buffer;\n";

    public static Status<OutputFileCollection> translateWithMainCallParameters(
            @NotNull MainCallParameters mainCall,
            @NotNull List<JetFile> files,
            @NotNull File outputFile,
            @Nullable File outputPrefixFile,
            @Nullable File outputPostfixFile,
            @NotNull Config config,
            @NotNull Consumer<JsNode> astConsumer // hack for tests
    ) throws TranslationException, IOException {
        K2JSTranslator translator = new K2JSTranslator(config);
        TextOutputImpl output = new TextOutputImpl();
        SourceMapBuilder sourceMapBuilder = config.isSourcemap() ? new SourceMap3Builder(outputFile, output, new SourceMapBuilderConsumer()) : null;
        Status<String> codeStatus = translator.generateProgramCode(files, mainCall, output, sourceMapBuilder, astConsumer);

        if (codeStatus.isFail()) return Status.fail();

        String programCode = codeStatus.getResult();
        String prefix = FileUtilsPackage.readTextOrEmpty(outputPrefixFile);
        String postfix = FileUtilsPackage.readTextOrEmpty(outputPostfixFile);

        List<File> sourceFiles = ContainerUtil.map(files, new Function<JetFile, File>() {
            @Override
            public File fun(JetFile file) {
                VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
                if (virtualFile == null) return new File(file.getName());
                return VfsUtilCore.virtualToIoFile(virtualFile);
            }
        });

        SimpleOutputFile jsFile = new SimpleOutputFile(sourceFiles, outputFile.getName(), prefix + programCode + postfix);
        List<SimpleOutputFile> outputFiles = new SmartList<SimpleOutputFile>(jsFile);

        if (sourceMapBuilder != null) {
            sourceMapBuilder.skipLinesAtBeginning(StringUtil.getLineBreakCount(prefix));
            SimpleOutputFile sourceMapFile = new SimpleOutputFile(sourceFiles, sourceMapBuilder.getOutFile().getName(), sourceMapBuilder.build());
            outputFiles.add(sourceMapFile);
        }

        OutputFileCollection outputFileCollection = new SimpleOutputFileCollection(outputFiles);
        return Status.success(outputFileCollection);
    }

    @NotNull
    private final Config config;

    public K2JSTranslator(@NotNull Config config) {
        this.config = config;
    }

    //NOTE: web demo related method
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public Status<String> translateStringWithCallToMain(@NotNull String programText, @NotNull String argumentsString) throws TranslationException {
        JetFile file = JetPsiFactory(getProject()).createFile("test", programText);
        Status<String> status = generateProgramCode(file, MainCallParameters.mainWithArguments(parseString(argumentsString)));

        if (status.isFail()) return status;

        String code = FLUSH_SYSTEM_OUT + status.getResult() + "\n" + GET_SYSTEM_OUT;
        return Status.success(code);
    }

    @NotNull
    public Status<String> generateProgramCode(@NotNull JetFile file, @NotNull MainCallParameters mainCallParameters) throws TranslationException {
        return generateProgramCode(Collections.singletonList(file), mainCallParameters);
    }

    @NotNull
    public Status<String> generateProgramCode(@NotNull List<JetFile> files, @NotNull MainCallParameters mainCallParameters)
            throws TranslationException {
        //noinspection unchecked
        return generateProgramCode(files, mainCallParameters, new TextOutputImpl(), null, Consumer.EMPTY_CONSUMER);
    }

    @NotNull
    public Status<String> generateProgramCode(
            @NotNull List<JetFile> files,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull TextOutputImpl output,
            @Nullable SourceMapBuilder sourceMapBuilder,
            @NotNull Consumer<JsNode> astConsumer
    ) throws TranslationException {
        JsProgram program = generateProgram(files, mainCallParameters);
        Diagnostics diagnostics = config.getTrace().getBindingContext().getDiagnostics();

        if (hasError(diagnostics)) return Status.fail();

        program = JsInliner.process(program);
        program.accept(new JsSourceGenerationVisitor(output, sourceMapBuilder));
        astConsumer.consume(program);

        return Status.success(output.toString());
    }

    @NotNull
    public JsProgram generateProgram(@NotNull List<JetFile> filesToTranslate,
            @NotNull MainCallParameters mainCallParameters)
            throws TranslationException {
        AnalysisResult analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(filesToTranslate, Predicates.<PsiFile>alwaysTrue(), config);
        BindingContext bindingContext = analysisResult.getBindingContext();
        TopDownAnalyzerFacadeForJS.checkForErrors(Config.withJsLibAdded(filesToTranslate, config), bindingContext);
        ModuleDescriptor moduleDescriptor = analysisResult.getModuleDescriptor();
        return Translation.generateAst(bindingContext, filesToTranslate, mainCallParameters, moduleDescriptor, config);
    }

    @NotNull
    private Project getProject() {
        return config.getProject();
    }
}
