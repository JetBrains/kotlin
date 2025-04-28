/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebFrontendPipelinePhase
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.test.frontend.fir.FirCliBasedOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade.Companion.shouldRunFirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputPartForDependsOnModule
import org.jetbrains.kotlin.test.frontend.fir.processErrorFromCliPhase
import org.jetbrains.kotlin.test.frontend.fir.toTestOutputPart
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

// TODO: Commonize with FirCliJvmFacade
class FirCliWebFacade(testServices: TestServices) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(cliBasedFacadesMarkerRegistrationData)

    override fun shouldTransform(module: TestModule): Boolean {
        return shouldRunFirFrontendFacade(module, testServices)
    }

    override fun analyze(module: TestModule): FirOutputArtifact? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val input = ConfigurationPipelineArtifact(
            configuration = configuration,
            diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector),
            rootDisposable = testServices.compilerConfigurationProvider.testRootDisposable,
        )

        val output = WebFrontendPipelinePhase.executePhase(input)
            ?: return processErrorFromCliPhase(configuration.messageCollector, testServices)

        val firOutputs = output.result.outputs
        val modulesFromTheSameStructure = module.transitiveDependsOnDependencies(includeSelf = true, reverseOrder = true)
            .associateBy { "<${it.name}>"}
        val testFirOutputs = firOutputs.map {
            val correspondingModule = modulesFromTheSameStructure.getValue(it.session.moduleData.name.asString())
            it.toTestOutputPart(correspondingModule, testServices)
        }

        return FirCliBasedWebOutputArtifact(output, testFirOutputs)
    }
}

class FirCliBasedWebOutputArtifact(
    val cliArtifact: WebFrontendPipelineArtifact,
    partsForDependsOnModules: List<FirOutputPartForDependsOnModule>,
) : FirOutputArtifact(partsForDependsOnModules), FirCliBasedOutputArtifact {
    override val diagnosticCollector: BaseDiagnosticsCollector
        get() = cliArtifact.diagnosticCollector
}

