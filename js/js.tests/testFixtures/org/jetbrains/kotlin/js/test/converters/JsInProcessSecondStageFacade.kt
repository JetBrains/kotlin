/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

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
import org.jetbrains.kotlin.js.test.blackbox.AbstractJsSecondStageGroupingFacade
import org.jetbrains.kotlin.js.test.blackbox.JsGroupedBatchArtifact
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
import java.io.File

/**
 * The second stage of the two-stage K/JS test pipeline that links with the current compiler, in-process, by means of
 * [JsUnifiedIrDeserializerAndLoweringFacade].
 */
class JsInProcessSecondStageFacade {
    /**
     * @param groupedBatchGranularities The granularities of the translation modes a grouped batch is linked in, out
     *   of the ones the settings of the tests ask for. An isolated test is always linked in all the modes it asks for.
     *   Beware that the name of a file generated in a per-module mode is made of the names of two modules, which are
     *   long in the grouping configuration, so it can exceed the limit of the file system for a test with a long name.
     */
    class Grouping(
        testServices: TestServices,
        private val groupedBatchGranularities: Set<JsGenerationGranularity>,
    ) : AbstractJsSecondStageGroupingFacade(testServices) {
        /** Links a grouped batch in the whole-program translation modes only. */
        constructor(testServices: TestServices) : this(testServices, setOf(JsGenerationGranularity.WHOLE_PROGRAM))

        override fun linkIsolated(output: NonGroupingStageOutput): BinaryArtifacts.Js? {
            val services = output.testServices
            val mainModule = JsEnvironmentConfigurator.getMainModule(services)
            val klib = services.artifactsProvider.getArtifact(mainModule, ArtifactKinds.KLib)
            if (JsEnvironmentConfigurator.incrementalEnabled(services)) {
                // The facade declares this service, but it is registered only for a facade that is a step of the test pipeline.
                services.register(JsIrIncrementalDataProvider::class, JsIrIncrementalDataProvider(services))
            }
            return JsUnifiedIrDeserializerAndLoweringFacade(services).transform(mainModule, klib)
        }

        override fun linkGroupedBatch(batch: GroupedBatch): BinaryArtifacts.Js? {
            val services = batch.services
            val launcherModule = batch.launcherModule
            val runtimeKlibs = JsEnvironmentConfigurator.getRuntimePathsForModule(launcherModule, services)

            val launcherKlibFile = batch.workingDir.resolve("launcher.klib")
            JsFirstStageInvoker.compileSourcesToKlib(
                sources = listOf(batch.launcherSource),
                klibOutputFile = launcherKlibFile,
                libraries = runtimeKlibs + batch.perTestKlibs,
            )

            val configuration = services.compilerConfigurationProvider.getCompilerConfiguration(launcherModule, CompilationStage.SECOND)
            configuration.includes = launcherKlibFile.canonicalPath
            configuration.libraries = runtimeKlibs + batch.perTestKlibs + launcherKlibFile.canonicalPath
            configuration.friendLibraries = emptyList()
            // The launcher exports its entry point itself, and `box()` of the test the configuration belongs to is not to be exported.
            configuration.additionalExportedDeclarationNames = emptySet()
            configuration.artifactConfigurations = configuration.artifactConfigurations.filter { it.granularity in groupedBatchGranularities }

            // The compiler reports the details of a failure to the message collector, which only prints them, while
            // the exception says nothing but the fact of the failure. A batch needs them to tell the test to blame.
            @OptIn(MessageCollectorAccess::class)
            val errorRecorder = configuration.messageCollector as? ErrorRecordingMessageCollector
                ?: ErrorRecordingMessageCollector(configuration.messageCollector).also { configuration.messageCollector = it }
            val launcherKlib = BinaryArtifacts.KLib(launcherKlibFile, DiagnosticsCollectorStub())
            val artifact = try {
                withTemporarySingleModuleStructure(services, launcherModule) {
                    JsUnifiedIrDeserializerAndLoweringFacade(services).transform(launcherModule, launcherKlib)
                }
            } catch (e: Throwable) {
                // The partial linkage engine reports to the diagnostics collector, which has not been flushed yet.
                CheckDiagnosticCollector.reportToMessageCollector(configuration)
                if (errorRecorder.messages.isEmpty()) throw e
                throw IllegalStateException(
                    "Linking of the grouped batch has failed: ${e.message}\n" +
                            errorRecorder.messages.joinToString("\n").take(MAX_COMPILER_MESSAGES_LENGTH),
                    e,
                )
            } ?: return null
            checkTestInfrastructure(artifact is JsIrArtifact) { "Unexpected artifact of a grouped batch: ${artifact::class}" }
            return JsGroupedBatchArtifact(artifact, launcherModule)
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
                // The partial linkage engine reports its issues as warnings, and then fails the compilation with an exception.
                if (severity.isError || severity.isWarning) messages += listOfNotNull(location?.path, message).joinToString(": ")
                delegate.report(severity, message, location)
            }
        }

        private companion object {
            const val MAX_COMPILER_MESSAGES_LENGTH = 16 * 1024
        }
    }
}
