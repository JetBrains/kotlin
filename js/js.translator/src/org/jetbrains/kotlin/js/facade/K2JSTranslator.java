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

import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.util.TextOutputImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFile;
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException;
import org.jetbrains.kotlin.js.inline.JsInliner;
import org.jetbrains.kotlin.js.sourceMap.JsSourceGenerationVisitor;
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder;
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilder;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.utils.fileUtils.FileUtilsPackage;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.DiagnosticUtils.hasError;
import static org.jetbrains.kotlin.js.facade.FacadeUtils.parseString;
import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

/**
 * An entry point of translator.
 */
public final class K2JSTranslator {

    public static final String FLUSH_SYSTEM_OUT = "Kotlin.out.flush();\n";
    public static final String GET_SYSTEM_OUT = "Kotlin.out.buffer;\n";

    @NotNull
    private final Config config;

    public K2JSTranslator(@NotNull Config config) {
        this.config = config;
    }

    @NotNull
    public TranslationResult translate(
            @NotNull List<JetFile> files,
            @NotNull MainCallParameters mainCallParameters
    ) throws TranslationException {
        JsAnalysisResult analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(files, config);
        BindingTrace bindingTrace = analysisResult.getBindingTrace();
        TopDownAnalyzerFacadeForJS.checkForErrors(Config.withJsLibAdded(files, config), bindingTrace.getBindingContext());
        ModuleDescriptor moduleDescriptor = analysisResult.getModuleDescriptor();
        TranslationContext context = Translation.generateAst(bindingTrace, files, mainCallParameters, moduleDescriptor, config);
        Diagnostics diagnostics = bindingTrace.getBindingContext().getDiagnostics();

        if (hasError(diagnostics)) return new TranslationResult.Fail(diagnostics);

        JsProgram program = JsInliner.process(context);
        return new TranslationResult.Success(config, files, program, diagnostics);
    }
}
