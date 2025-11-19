/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.ModuleCompilerEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertAnalyzedFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.generateCodeFromIr
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import kotlin.script.experimental.api.*

class ScriptJvmK2Compiler(
    state: K2ScriptingCompilerEnvironment,
    private val convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile
) : ScriptCompilerProxy {

    private val state = (state as? K2ScriptingCompilerEnvironmentInternal) ?: error("Expected the state of type K2ScriptingCompilerEnvironmentInternal, got ${state::class}")

    fun compile(script: SourceCode) = compile(script, state.baseScriptCompilationConfiguration)

    @OptIn(LegacyK2CliPipeline::class)
    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<CompiledScript> {

        val project = state.projectEnvironment.project
        val messageCollector = state.messageCollector
        val diagnosticsCollector = DiagnosticReporterFactory.createPendingReporter(messageCollector)

        val compilerConfiguration = state.compilerContext.environment.configuration.copy().apply {
            jvmTarget = selectJvmTarget(scriptCompilationConfiguration, messageCollector)
        }
        val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
        val compilerEnvironment = ModuleCompilerEnvironment(state.projectEnvironment, diagnosticsCollector)
        val renderDiagnosticName = compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        val targetId = TargetId(script.name!!, "java-production")


        return scriptCompilationConfiguration.refineAll(script, diagnosticsCollector, convertToFir).onSuccess { refinedConfiguration ->
            // TODO: separate reporter for refinement, to avoid double warnings reporting
            // imports -> compile one by one or all here?
            // add all deps
            // create source session (with dependent one if 1.1
            val moduleData = state.moduleDataProvider.addNewSscriptModuleData(Name.special("<script-${script.name!!}>"))
            val session = FirJvmSessionFactory.createSourceSession(
                moduleData,
                AbstractProjectFileSearchScope.EMPTY,
                createIncrementalCompilationSymbolProviders = { null },
                extensionRegistrars,
                compilerConfiguration,
                // TODO: from script config
                context = state.sessionFactoryContext,
                needRegisterJavaElementFinder = true,
                isForLeafHmppModule = false,
                init = {},
            )

            val rawFir = script.convertToFir(session, diagnosticsCollector)

            val outputs = listOf(resolveAndCheckFir(session, listOf(rawFir), diagnosticsCollector)).also {
                it.runPlatformCheckers(diagnosticsCollector)
            }
            val frontendOutput = AllModulesFrontendOutput(outputs)


            if (diagnosticsCollector.hasErrors) {
                diagnosticsCollector.reportToMessageCollector(messageCollector, renderDiagnosticName)
                return failure(messageCollector)
            }

            val irInput = convertAnalyzedFirToIr(compilerConfiguration, targetId, frontendOutput, compilerEnvironment)

            if (diagnosticsCollector.hasErrors) {
                diagnosticsCollector.reportToMessageCollector(messageCollector, renderDiagnosticName)
                return failure(messageCollector)
            }

            val generationState = generateCodeFromIr(irInput, compilerEnvironment)

            diagnosticsCollector.reportToMessageCollector(messageCollector, renderDiagnosticName)

            if (diagnosticsCollector.hasErrors) {
                return failure(messageCollector)
            }
//            refinedConfiguration.asSuccess()
            TODO()
        }
    }
}

private fun ScriptCompilationConfiguration.refineAll(
    script: SourceCode,
    diagnosticsCollector: BaseDiagnosticsCollector,
    convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    refineBeforeParsing(script).onSuccess {
        val collectedData by lazy(LazyThreadSafetyMode.NONE) {
            ScriptCollectedData(
                mapOf(
                    ScriptCollectedData.fir to listOf(script.convertToFir(createDummySessionForScriptRefinement(script), diagnosticsCollector))
                )
            )
        }
        it.refineOnFir(script, collectedData)
    }.onSuccess {
        it.refineBeforeCompiling(script)
    }

@OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
private fun createDummySessionForScriptRefinement(script: SourceCode): FirSession =
    object : FirSession(Kind.Source) {}.apply {
        val moduleData = FirSourceModuleData(
            Name.identifier("<${script.name}stub module for script refinement>"),
            dependencies = emptyList(),
            dependsOnDependencies = emptyList(),
            friendDependencies = emptyList(),
            platform = JvmPlatforms.unspecifiedJvmPlatform,
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
        register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(
                LanguageVersionSettingsImpl.DEFAULT,
                isMetadataCompilation = false
            ))
        register(FirExtensionService::class, FirExtensionService(this))
    }

