/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.pipeline.PipelineStepException
import org.jetbrains.kotlin.cli.pipeline.SuccessfulPipelineExecutionException
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt.compileForStubGeneration
import org.jetbrains.kotlin.kapt.util.CompilerConfigurationBackedKaptLogger
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

class JvmCompilerWithKaptFacade(private val testServices: TestServices) :
    AbstractTestFacade<ResultingArtifact.Source, KaptContextBinaryArtifact>() {
    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind
    override val outputKind: TestArtifactKind<KaptContextBinaryArtifact>
        get() = KaptContextBinaryArtifact.Kind

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(cliBasedFacadesMarkerRegistrationData)

    override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): KaptContextBinaryArtifact {
        val configurationProvider = testServices.compilerConfigurationProvider
        val configuration = configurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
        val logger = CompilerConfigurationBackedKaptLogger(isVerbose = true, isInfoAsWarnings = false, configuration)
        val kaptContext = try {
            compileForStubGeneration(
                configurationProvider.testRootDisposable,
                configuration,
                testServices.kaptOptionsProvider[module],
                logger,
                withJdk = true,
            )
        } catch (_: SuccessfulPipelineExecutionException) {
            // In the integration tests KAPT is registered as a FIR analysis handler extension, which means that it has already
            // performed the whole stub generation and annotation processing itself, and the frontend phase has stopped the
            // pipeline. There is nothing left to compile, so there is no KAPT context either.
            return KaptContextBinaryArtifact(kaptContext = null)
        } catch (_: PipelineStepException) {
            throw CompilationErrorException()
        }
        return KaptContextBinaryArtifact(kaptContext ?: throw CompilationErrorException())
    }

    override fun shouldTransform(module: TestModule): Boolean {
        return true // TODO
    }
}

class KaptContextBinaryArtifact(val kaptContext: KaptContextForStubGeneration?) : ResultingArtifact.Binary<KaptContextBinaryArtifact>() {
    object Kind : ArtifactKind<KaptContextBinaryArtifact>("KaptArtifact", CompilationStage.FIRST)

    override val kind: ArtifactKind<KaptContextBinaryArtifact>
        get() = Kind
}
