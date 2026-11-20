/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors.CheckDiagnosticCollector
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.additionalExportedDeclarationNames
import org.jetbrains.kotlin.js.config.artifactConfigurations
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.js.test.converters.JsUnifiedIrDeserializerAndLoweringFacade
import org.jetbrains.kotlin.js.test.converters.incremental.RecompileModuleJsIrBackendFacade
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.diagnostics.DiagnosticsCollectorStub
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.JsIrArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service
import java.io.File

/**
 * The second stage of the two-stage K/JS test pipeline that links with the current compiler, in-process, by means of
 * [JsUnifiedIrDeserializerAndLoweringFacade].
 */
class JsInProcessSecondStageFacade {
    /**
     * Links a grouped batch in the translation modes the settings of the tests ask for, but of the granularities
     * [linkPerModule] allows only; an isolated test is linked in all the modes it asks for.
     *
     * @param recompile whether an isolated test that has files to recompile is then recompiled incrementally, as
     *   `RecompileModuleJsIrBackendFacade` does it in the one-stage pipeline. A test with such files is never grouped.
     * @param linkPerModule whether a grouped batch is linked in the per-module modes as well as in the whole-program
     *   ones. A per-module mode produces a file per module, named after the first test of the batch, the launcher
     *   module and that module. The names of the modules of the tests are long in the grouping configuration, so the
     *   launcher module is renamed to [LAUNCHER_MODULE_NAME], and the KLIBs of the tests are to be compiled with the
     *   short JS names of [JsGroupedTestModuleOutputNameConfigurator], which keeps the names of the files well within
     *   the limit of the file system of 255 characters.
     *   A batch is never linked in the per-file modes, which are for the ES modules only, and so for isolated tests only.
     */
    class Grouping(
        testServices: TestServices,
        private val recompile: Boolean = false,
        private val linkPerModule: Boolean = false,
    ) : AbstractJsSecondStageGroupingFacade(testServices) {
        private val linkedGranularities: Set<JsGenerationGranularity> = buildSet {
            add(JsGenerationGranularity.WHOLE_PROGRAM)
            if (linkPerModule) add(JsGenerationGranularity.PER_MODULE)
        }

        override fun linkIsolated(output: NonGroupingStageOutput): BinaryArtifacts.Js? {
            val services = output.testServices
            val mainModule = JsEnvironmentConfigurator.getMainModule(services)
            val klib = services.artifactsProvider.getArtifact(mainModule, ArtifactKinds.KLib)
            if (JsEnvironmentConfigurator.incrementalEnabled(services)) {
                // The facade declares this service, but it is registered only for a facade that is a step of the test
                // pipeline. A first stage that has recorded its incremental data into the service must keep it.
                services.register(service(::JsIrIncrementalDataProvider), skipAlreadyRegistered = true)
            }
            val artifact = JsUnifiedIrDeserializerAndLoweringFacade(services).transform(mainModule, klib) ?: return null
            if (!recompile) return artifact
            val recompileFacade = RecompileModuleJsIrBackendFacade(services)
            return if (recompileFacade.shouldTransform(mainModule)) recompileFacade.transform(mainModule, artifact) else artifact
        }

        override fun linkGroupedBatch(batch: GroupedBatch): BinaryArtifacts.Js? {
            val services = batch.services
            val launcherModule = if (linkPerModule) batch.launcherModule.copy(name = LAUNCHER_MODULE_NAME) else batch.launcherModule
            val runtimeKlibs = JsEnvironmentConfigurator.getRuntimePathsForModule(launcherModule, services)

            val launcherKlibFile = batch.workingDir.resolve("launcher.klib")
            JsFirstStageInvoker.compileSourcesToKlib(
                sources = listOf(batch.launcherSource),
                klibOutputFile = launcherKlibFile,
                libraries = runtimeKlibs + batch.perTestKlibs,
            )
            val launcherKlib = BinaryArtifacts.KLib(launcherKlibFile, DiagnosticsCollectorStub())
            if (launcherModule.name != batch.launcherModule.name) {
                // The configuration of a module is made from its KLIB, and the renamed launcher module has no other KLIB than this one.
                services.artifactsProvider.registerArtifact(launcherModule, launcherKlib)
            }

            val configuration = services.compilerConfigurationProvider.getCompilerConfiguration(launcherModule, CompilationStage.SECOND)
            configuration.includes = launcherKlibFile.canonicalPath
            configuration.libraries = runtimeKlibs + batch.perTestKlibs + launcherKlibFile.canonicalPath
            configuration.friendLibraries = emptyList()
            // The launcher exports its entry point itself, and `box()` of the test the configuration belongs to is not to be exported.
            configuration.additionalExportedDeclarationNames = emptySet()
            configuration.artifactConfigurations = configuration.artifactConfigurations.filter { it.granularity in linkedGranularities }

            // The compiler reports the details of a failure to the message collector, which only prints them, while
            // the exception says nothing but the fact of the failure. Every test of the batch is failed with them,
            // so that the test to blame can be told from the paths they carry.
            @OptIn(MessageCollectorAccess::class)
            val errorRecorder = configuration.messageCollector as? ErrorRecordingMessageCollector
                ?: ErrorRecordingMessageCollector(configuration.messageCollector).also { configuration.messageCollector = it }
            val artifact = try {
                withTemporarySingleModuleStructure(services, launcherModule) {
                    JsUnifiedIrDeserializerAndLoweringFacade(services).transform(launcherModule, launcherKlib)
                }
            } catch (e: Throwable) {
                // The compiler reports to the diagnostics collector, which has not been flushed yet.
                CheckDiagnosticCollector.reportToMessageCollector(configuration)
                // Warnings are recorded as well (see `ErrorRecordingMessageCollector`), so the recorded messages are rarely
                // empty, and they may include warnings unrelated to the failure.
                if (errorRecorder.messages.isEmpty()) throw e
                throw IllegalStateException(
                    "Linking of the grouped batch has failed: ${e.message}\n" +
                            errorRecorder.messages.joinToString("\n").take(MAX_COMPILER_MESSAGES_LENGTH),
                    e,
                )
            } ?: return null
            checkTestInfrastructure(artifact is JsIrArtifact) { "Unexpected artifact of a grouped batch: ${artifact::class}" }
            return JsGroupedBatchArtifact(artifact, launcherModule, linkPerModule)
        }

        /**
         * The facades find the main module, which they are to link, in the module structure, so it has to consist of
         * the synthetic launcher module for the time of linking.
         */
        private inline fun <T> withTemporarySingleModuleStructure(services: TestServices, module: TestModule, action: () -> T): T {
            val originalModuleStructure = services.moduleStructure
            val temporaryModuleStructure = object : TestModuleStructure() {
                override val modules: List<TestModule> = listOf(module)
                override val allDirectives = originalModuleStructure.allDirectives
                override val originalTestDataFiles: List<File> = originalModuleStructure.originalTestDataFiles
            }

            services.register(TestModuleStructure::class, temporaryModuleStructure)
            return try {
                action()
            } finally {
                services.register(TestModuleStructure::class, originalModuleStructure)
            }
        }

        private class ErrorRecordingMessageCollector(private val delegate: MessageCollector) : MessageCollector by delegate {
            val messages = mutableListOf<String>()

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                // Warnings are recorded as well, since they may explain the failure.
                if (severity.isError || severity.isWarning) messages += listOfNotNull(location?.path, message).joinToString(": ")
                delegate.report(severity, message, location)
            }
        }

        private companion object {
            const val MAX_COMPILER_MESSAGES_LENGTH = 16 * 1024
            const val LAUNCHER_MODULE_NAME = "launcher"
        }
    }
}
