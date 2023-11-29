/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KtReadActionConfinementLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.KaptOptions
import org.jetbrains.kotlin.kapt3.base.javac.reportKaptError
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.jetbrains.kotlin.kapt3.test.KaptMessageCollectorProvider
import org.jetbrains.kotlin.kapt3.test.kaptOptionsProvider
import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.jvm.JvmKotlinp
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
        val (context, stubMap) = run(
            configuration,
            options,
            configurationProvider.testRootDisposable
        )
        return Kapt4ContextBinaryArtifact(context, stubMap.values.filterNotNull())
    }

    private fun TestFile.realFile(): File {
        return testServices.sourceFileProvider.getRealFileForSourceFile(this)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true
    }
}

@OptIn(KtAnalysisApiInternals::class)
private fun run(
    configuration: CompilerConfiguration,
    options: KaptOptions,
    projectDisposable: Disposable,
): Pair<KaptContext, Map<KtLightClass, KaptStub?>> {
    val standaloneAnalysisAPISession = buildStandaloneAnalysisAPISession(projectDisposable) {
        (project as MockProject).registerService(
            KtLifetimeTokenProvider::class.java,
            KtReadActionConfinementLifetimeTokenProvider::class.java
        )
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

    return context to generateStubs(module, files, options, logger, metadataRenderer = { renderMetadata(it) })
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
    val text = JvmKotlinp(Settings(isVerbose = true, sortDeclarations = true)).printClassFile(KotlinClassMetadata.readLenient(metadata))
    // "/*" and "*/" delimiters are used in kotlinp, for example to render type parameter names. Replace them with something else
    // to avoid them being interpreted as Java comments.
    val sanitized = text.split('\n')
        .dropLast(1)
        .map {
            it.replace("/*", "(*").replace("*/", "*)")
        }
    println("/**")
    sanitized.forEach {
        println(" * ", it)
    }
    println(" */")
    println("@kotlin.Metadata()")
}
