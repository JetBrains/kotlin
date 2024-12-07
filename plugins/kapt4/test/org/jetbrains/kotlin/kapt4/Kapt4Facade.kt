/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.KaptOptions
import org.jetbrains.kotlin.kapt3.base.javac.reportKaptError
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.jetbrains.kotlin.kapt3.test.KaptMessageCollectorProvider
import org.jetbrains.kotlin.kapt3.test.handlers.renderNormalizedMetadata
import org.jetbrains.kotlin.kapt3.test.kaptOptionsProvider
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.File
import java.io.PrintWriter

internal class Kapt4Facade(private val testServices: TestServices) :
    AbstractTestFacade<ResultingArtifact.Source, Kapt4ContextBinaryArtifact>() {
    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind
    override val outputKind: TestArtifactKind<Kapt4ContextBinaryArtifact>
        get() = Kapt4ContextBinaryArtifact.Kind

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::KaptMessageCollectorProvider))

    override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): Kapt4ContextBinaryArtifact {
        val configurationProvider = testServices.compilerConfigurationProvider

        val configuration = configurationProvider.getCompilerConfiguration(module)
        configuration.addKotlinSourceRoots(module.files.filter { it.isKtFile }.map { it.realFile().absolutePath })
        val options = testServices.kaptOptionsProvider[module]
        val (context, stubs) = run(
            configuration,
            options,
            configurationProvider.testRootDisposable
        )
        return Kapt4ContextBinaryArtifact(context, stubs)
    }

    private fun TestFile.realFile(): File {
        return testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(this)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true
    }
}

private fun run(
    configuration: CompilerConfiguration,
    options: KaptOptions,
    projectDisposable: Disposable,
): Pair<KaptContext, List<KaptStub>> {
    val standaloneAnalysisAPISession = buildStandaloneAnalysisAPISession(projectDisposable) {
        @Suppress("DEPRECATION")
        buildKtModuleProviderByCompilerConfiguration(configuration)
    }
    val (module, files) = standaloneAnalysisAPISession.modulesWithFiles.entries.single()
    val context = KaptContext(
        options,
        withJdk = false,
        WriterBackedKaptLogger(isVerbose = false),
    )

    val logger = object : KaptLogger {
        override val errorWriter: PrintWriter
            get() = shouldNotBeCalled()
        override val infoWriter: PrintWriter
            get() = shouldNotBeCalled()
        override val isVerbose: Boolean
            get() = shouldNotBeCalled()
        override val warnWriter: PrintWriter
            get() = shouldNotBeCalled()

        override fun error(message: String) {
            context.reportKaptError(*message.split("\n").toTypedArray())
        }

        override fun exception(e: Throwable) {
            error(e.toString())
        }

        override fun info(message: String) {
        }

        override fun warn(message: String) {
            error(message)
        }
    }

    val stubsMap = generateStubs(module, files, options, logger, MetadataVersion.INSTANCE) { renderMetadata(it) }
    return context to stubsMap.entries.sortedBy { it.key.qualifiedName }.mapNotNull { it.value }
}

internal data class Kapt4ContextBinaryArtifact(
    internal val kaptContext: KaptContext,
    internal val kaptStubs: List<KaptStub>
) : ResultingArtifact.Binary<Kapt4ContextBinaryArtifact>() {
    object Kind : BinaryKind<Kapt4ContextBinaryArtifact>("KaptArtifact")

    override val kind: BinaryKind<Kapt4ContextBinaryArtifact>
        get() = Kind
}

private fun Printer.renderMetadata(metadata: Metadata) {
    println("/**")
    for (line in renderNormalizedMetadata(metadata)) {
        println(" * ", line)
    }
    println(" */")
    println("@kotlin.Metadata()")
}
