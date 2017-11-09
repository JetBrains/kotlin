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

package org.jetbrains.kotlin.annotation

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAll
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.org.objectweb.asm.ClassWriter
import java.io.File

class StubProducerExtension(
        private val stubsOutputDir: File,
        private val messageCollector: MessageCollector,
        private val reportOutputFiles: Boolean
) : AnalysisHandlerExtension {
    override fun analysisCompleted(
            project: Project,
            module: ModuleDescriptor,
            bindingTrace: BindingTrace,
            files: Collection<KtFile>
    ): AnalysisResult? {
        val generationState = GenerationState(
                project, 
                StubClassBuilderFactory(),
                module, 
                bindingTrace.bindingContext, 
                files.toList(),
                CompilerConfiguration.EMPTY)

        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)

        if (!stubsOutputDir.exists()) stubsOutputDir.mkdirs()
        generationState.factory.writeAll(stubsOutputDir, messageCollector, reportOutputFiles)

        generationState.destroy()
        return AnalysisResult.success(BindingContext.EMPTY, module, shouldGenerateCode = false)
    }
}

private class StubClassBuilderFactory : ClassBuilderFactory {

    override fun getClassBuilderMode() = ClassBuilderMode.KAPT

    override fun newClassBuilder(origin: JvmDeclarationOrigin) = AbstractClassBuilder.Concrete(
            ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS))

    override fun asText(builder: ClassBuilder) = throw UnsupportedOperationException("BINARIES generator asked for text")

    override fun asBytes(builder: ClassBuilder): ByteArray {
        val visitor = builder.visitor as ClassWriter
        return visitor.toByteArray()
    }

    override fun close() {

    }
}
