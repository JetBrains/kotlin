/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KtReadActionConfinementLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.jetbrains.kotlin.kapt3.test.KaptMessageCollectorProvider
import org.jetbrains.kotlin.kapt3.test.kaptOptionsProvider
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import java.io.File

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
            testServices.applicationDisposableProvider.getApplicationRootDisposable(),
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
    applicationDisposable: Disposable,
    projectDisposable: Disposable,
): Pair<Kapt4ContextForStubGeneration, Map<KtLightClass, Kapt4StubGenerator.KaptStub?>> {
    val standaloneAnalysisAPISession = buildStandaloneAnalysisAPISession(applicationDisposable, projectDisposable) {
        (project as MockProject).registerService(
            KtLifetimeTokenProvider::class.java,
            KtReadActionConfinementLifetimeTokenProvider::class.java
        )
        @Suppress("DEPRECATION")
        buildKtModuleProviderByCompilerConfiguration(configuration)
    }
    val (module, psiFiles) = standaloneAnalysisAPISession.modulesWithFiles.entries.single()
    val ktFiles = psiFiles.filterIsInstance<KtFile>()

    val lightClasses = buildSet {
        ktFiles.flatMapTo(this) { file ->
            file.children.filterIsInstance<KtClassOrObject>().mapNotNull {
                it.toLightClass()
            }
        }
        ktFiles.mapNotNullTo(this) { ktFile -> ktFile.findFacadeClass() }.distinct()
    }

    return KtAnalysisSessionProvider.getInstance(module.project).analyze(module) {
        val context = Kapt4ContextForStubGeneration(
            options,
            withJdk = false,
            WriterBackedKaptLogger(isVerbose = false),
            this@analyze,
            lightClasses
        )
        val generator = with(context) { Kapt4StubGenerator() }
        context to generator.generateStubs()
    }
}

internal data class Kapt4ContextBinaryArtifact(
    internal val kaptContext: Kapt4ContextForStubGeneration,
    internal val kaptStubs: List<Kapt4StubGenerator.KaptStub>
) : ResultingArtifact.Binary<Kapt4ContextBinaryArtifact>() {
    object Kind : BinaryKind<Kapt4ContextBinaryArtifact>("KaptArtifact")

    override val kind: BinaryKind<Kapt4ContextBinaryArtifact>
        get() = Kind
}

