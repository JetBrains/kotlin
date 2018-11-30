/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.incremental.isClassFile
import org.jetbrains.kotlin.jvm.abi.asm.AbiClassBuilder
import org.jetbrains.kotlin.jvm.abi.asm.FilterInnerClassesVisitor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.org.objectweb.asm.*
import java.io.File

class JvmAbiAnalysisHandlerExtension(
    private val compilerConfiguration: CompilerConfiguration
) : AnalysisHandlerExtension {
    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        val bindingContext = bindingTrace.bindingContext
        if (bindingContext.diagnostics.any { it.severity == Severity.ERROR }) return null

        val targetId = TargetId(
            name = compilerConfiguration[CommonConfigurationKeys.MODULE_NAME] ?: module.name.asString(),
            type = "java-production"
        )

        val generationState = GenerationState.Builder(
            project,
            AbiBinaries,
            module,
            bindingContext,
            files.toList(),
            compilerConfiguration
        ).targetId(targetId).build()
        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)

        val outputDir = compilerConfiguration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)!!
        val outputFiles = arrayListOf<File>()
        generationState.factory.writeAll(
            outputDir,
            fun(file: OutputFile, sources: List<File>, output: File) {
                // todo report
                outputFiles.add(output)
            }
        )

        val classFiles = HashSet<File>()
        val removedClasses = HashSet<String>()
        for (file in outputFiles) {
            if (!file.isClassFile()) continue

            classFiles.add(file)
            val localFileKotlinClass = LocalFileKotlinClass.create(file) ?: continue
            val header = localFileKotlinClass.classHeader
            if (header.kind == KotlinClassHeader.Kind.CLASS) {
                val (_, classProto) = JvmProtoBufUtil.readClassDataFrom(header.data!!, header.strings!!)

                val visibility = Flags.VISIBILITY.get(classProto.flags)
                if (visibility == ProtoBuf.Visibility.PRIVATE || visibility == ProtoBuf.Visibility.LOCAL) {
                    file.delete()
                    classFiles.remove(file)
                    removedClasses.add(localFileKotlinClass.className.internalName)
                }
            }
        }

        for (file in classFiles) {
            val reader = ClassReader(file.readBytes())
            val writer = ClassWriter(reader, 0)
            val visitor = FilterInnerClassesVisitor(removedClasses, Opcodes.ASM6, writer)
            reader.accept(visitor, 0)
            file.writeBytes(writer.toByteArray())
        }

        return null
    }

    private object AbiBinaries : ClassBuilderFactory {
        override fun getClassBuilderMode(): ClassBuilderMode =
            ClassBuilderMode.ABI

        override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder =
            AbiClassBuilder(ClassWriter(0))

        override fun asText(builder: ClassBuilder): String =
            throw UnsupportedOperationException("AbiBinaries generator asked for text")

        override fun asBytes(builder: ClassBuilder): ByteArray {
            val visitor = builder.visitor as ClassWriter
            return visitor.toByteArray()
        }

        override fun close() {}
    }
}
