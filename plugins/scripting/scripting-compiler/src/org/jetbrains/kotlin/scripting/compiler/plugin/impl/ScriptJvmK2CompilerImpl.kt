/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForLightTree
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.ModuleCompilerEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertAnalyzedFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.generateCodeFromIr
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.createSourceSession
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.readSourceFileWithMapping
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.configureFirSession
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.getOrStoreRefinedCompilationConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.getRefinedOrBaseCompilationConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.scriptRefinedCompilationConfigurationsCache
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.collectScriptsCompilationDependenciesRecursively
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.FirScriptCompilationComponent
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.impl._languageVersion
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty

class ScriptJvmK2CompilerIsolated(val hostConfiguration: ScriptingHostConfiguration) : ScriptCompilerProxy {
    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<CompiledScript> =
        withMessageCollector { messageCollector ->
            withScriptCompilationCache(script, scriptCompilationConfiguration, messageCollector) {
                withK2ScriptCompilerWithLightTree(
                    scriptCompilationConfiguration.with {
                        hostConfiguration(this@ScriptJvmK2CompilerIsolated.hostConfiguration)
                    },
                    messageCollector
                ) {
                    if (messageCollector.hasErrors()) failure(messageCollector)
                    else it.compile(script)
                }
            }
        }
}

class ScriptJvmK2CompilerFromEnvironment(
    val environment: KotlinCoreEnvironment,
    val hostConfiguration: ScriptingHostConfiguration,
) : ScriptCompilerProxy {
    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<CompiledScript> =
        withMessageCollector(script = script) { messageCollector ->
            val configWithUpdatedHost = scriptCompilationConfiguration.updateWithHostConfiguration(hostConfiguration)
            withScriptCompilationCache(script, configWithUpdatedHost, messageCollector) {
                val compiler = ScriptJvmK2CompilerImpl(
                    createCompilerStateFromEnvironment(environment, messageCollector, configWithUpdatedHost, hostConfiguration),
                    SourceCode::convertToFirViaLightTree
                )
                if (messageCollector.hasErrors()) failure(messageCollector)
                else compiler.compile(script)
            }
        }
}

fun ScriptCompilationConfiguration.updateWithHostConfiguration(hostConfiguration: ScriptingHostConfiguration) =
    with {
        val providedHostConfiguration = this[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
        hostConfiguration(
            hostConfiguration.withDefaultsFrom(providedHostConfiguration)
        )
    }

class ScriptJvmK2CompilerImpl(
    state: K2ScriptingCompilerEnvironment,
    private val convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile
) {

    private val state = (state as? K2ScriptingCompilerEnvironmentInternal)
        ?: error("Expected the state of type K2ScriptingCompilerEnvironmentInternal, got ${state::class}")

    fun compile(script: SourceCode) = compile(script, state.baseScriptCompilationConfiguration)

    private class ErrorReportingContext(
        val messageCollector: ScriptDiagnosticsMessageCollector,
        val diagnosticsCollector: BaseDiagnosticsCollector,
        val renderDiagnosticName: Boolean,
    )

    fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<CompiledScript> = context(
        ErrorReportingContext(
            state.messageCollector,
            DiagnosticsCollectorImpl(),
            state.compilerContext.environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )
    ) {
        if (state.compilerContext.environment.configuration.languageVersionSettings.languageVersion.major < 2)
            makeFailureResult("This script compiler implementatione is not compatible with Kotlin 1.9 and earlier")
        else scriptCompilationConfiguration.refineAll(script)
            .onSuccess {
                compileImpl(script, it)
            }
    }

    @OptIn(SessionConfiguration::class)
    private fun ScriptCompilationConfiguration.refineAll(
        script: SourceCode,
    ): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        refineAllForK2(script, state.hostConfiguration) { source, configuration ->
            collectAndResolveScriptAnnotationsViaFir(
                source, configuration, state.hostConfiguration,
                { _, scriptCompilationConfiguration -> state.getOrCreateSessionForAnnotationResolution(scriptCompilationConfiguration) },
                { session, diagnosticsReporter -> convertToFir(session, diagnosticsReporter) }
            )
        }.onSuccess {
            it.with {
                _languageVersion(state.compilerContext.environment.configuration.languageVersionSettings.languageVersion.versionString)
            }.asSuccess()
        }

    context(reportingCtx: ErrorReportingContext)
    private fun failure(
        diagnosticsCollector: BaseDiagnosticsCollector,
        vararg diagnostics: ScriptDiagnostic
    ): ResultWithDiagnostics.Failure {
        diagnosticsCollector.reportToMessageCollector(reportingCtx.messageCollector, reportingCtx.renderDiagnosticName)
        return ResultWithDiagnostics.Failure(*reportingCtx.messageCollector.diagnostics.toTypedArray<ScriptDiagnostic>(), *diagnostics)
    }

    @OptIn(LegacyK2CliPipeline::class, DirectDeclarationsAccess::class, SessionConfiguration::class)
    context(reportingCtx: ErrorReportingContext)
    private fun compileImpl(
        script: SourceCode,
        scriptRefinedCompilationConfiguration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<CompiledScript> {

        val compilerConfiguration = state.compilerContext.environment.configuration.copy().apply {
            jvmTarget = selectJvmTarget(scriptRefinedCompilationConfiguration, reportingCtx.messageCollector)
        }

        state.hostConfiguration[ScriptingHostConfiguration.scriptRefinedCompilationConfigurationsCache]
            ?.storeRefinedCompilationConfiguration(
                script,
                scriptRefinedCompilationConfiguration.asSuccess()
            )

        val allSourceFiles = mutableListOf(script)
        val (classpath, newSources, sourceDependencies) =
            collectScriptsCompilationDependenciesRecursively(allSourceFiles) { importedScript ->
                state.hostConfiguration.getOrStoreRefinedCompilationConfiguration(importedScript) { source, baseConfig ->
                    baseConfig.refineAll(source)
                }
            }.valueOr { return it }
        allSourceFiles.addAll(newSources)

        val ignoredOptionsReportingState = state.compilerContext.ignoredOptionsReportingState
        val updatedCompilerOptions = allSourceFiles.flatMapTo(mutableListOf()) {
            getRefinedConfiguration(it)[ScriptCompilationConfiguration.compilerOptions] ?: emptyList()
        }
        if (updatedCompilerOptions.isNotEmpty() && updatedCompilerOptions != state.baseScriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions]) {
            compilerConfiguration.updateWithCompilerOptions(
                updatedCompilerOptions,
                reportingCtx.messageCollector,
                ignoredOptionsReportingState,
                true
            )
        }

        if (reportingCtx.messageCollector.hasErrors()) return failure(reportingCtx.diagnosticsCollector)

        configureLibrarySessionIfNeeded(state, compilerConfiguration, classpath)

        val compilerEnvironment = ModuleCompilerEnvironment(state.projectEnvironment, reportingCtx.diagnosticsCollector)
        val renderDiagnosticName = compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        val targetId = TargetId(script.name ?: "main", "java-production")

        val moduleData = state.moduleDataProvider.addNewScriptModuleData(Name.special("<script-${script.name ?: "main"}>"))

        val session = createSourceSession(
            moduleData,
            AbstractProjectFileSearchScope.EMPTY,
            createIncrementalCompilationSymbolProviders = { null },
            state.extensionRegistrars,
            compilerConfiguration,
            context = state.sessionFactoryContext,
            needRegisterJavaElementFinder = true,
            isForLeafHmppModule = false,
            init = {},
        )

        session.register(
            FirScriptCompilationComponent::class,
            FirScriptCompilationComponent(
                state.hostConfiguration,
                getSessionForAnnotationResolution = { _, scriptCompilationConfiguration ->
                    state.getOrCreateSessionForAnnotationResolution(
                        scriptCompilationConfiguration
                    )
                }
            )
        )

        state.hostConfiguration[ScriptingHostConfiguration.configureFirSession]?.also {
            it.invoke(session)
        }

        val sourcesToFir = allSourceFiles.associateWith { it.convertToFir(session, reportingCtx.diagnosticsCollector) }

        if (reportingCtx.diagnosticsCollector.hasErrors) return failure(reportingCtx.diagnosticsCollector)

        checkKotlinPackageUsageForLightTree(compilerConfiguration, sourcesToFir.values)

        if (reportingCtx.messageCollector.hasErrors()) return failure(reportingCtx.diagnosticsCollector)

        val outputs = listOf(resolveAndCheckFir(session, sourcesToFir.values.toList(), reportingCtx.diagnosticsCollector)).also {
            it.runPlatformCheckers(reportingCtx.diagnosticsCollector)
        }
        val frontendOutput = AllModulesFrontendOutput(outputs)

        if (reportingCtx.diagnosticsCollector.hasErrors) return failure(reportingCtx.diagnosticsCollector)

        val irInput = convertAnalyzedFirToIr(compilerConfiguration, targetId, frontendOutput, compilerEnvironment)

        if (reportingCtx.diagnosticsCollector.hasErrors) return failure(reportingCtx.diagnosticsCollector)

        val generationState = generateCodeFromIr(irInput, compilerEnvironment)

        reportingCtx.diagnosticsCollector.reportToMessageCollector(reportingCtx.messageCollector, renderDiagnosticName)

        if (reportingCtx.diagnosticsCollector.hasErrors) {
            return failure(reportingCtx.diagnosticsCollector)
        }

        return makeCompiledScript(
            generationState,
            script,
            {
                sourcesToFir[it]?.declarations?.firstIsInstanceOrNull<FirScript>()
                    ?.let { it.symbol.packageFqName().child(NameUtils.getScriptTargetClassName(it.name)) }
            },
            sourceDependencies,
            ::getRefinedConfiguration,
            extractResultFields(irInput.irModuleFragment)
        ).onSuccess { compiledScript ->
            ResultWithDiagnostics.Success(compiledScript, reportingCtx.messageCollector.diagnostics)
        }
    }

    private fun getRefinedConfiguration(script: SourceCode): ScriptCompilationConfiguration =
        state.hostConfiguration.getRefinedOrBaseCompilationConfiguration(script).valueOrThrow() // TODO: errors? orBase?

}

fun <T> withK2ScriptCompilerWithLightTree(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    parentMessageCollector: MessageCollector? = null,
    body: (ScriptJvmK2CompilerImpl) -> T,
): T {
    val disposable = Disposer.newDisposable("Default disposable for scripting compiler")
    return try {
        body(
            ScriptJvmK2CompilerImpl(
                createIsolatedCompilerState(
                    ScriptDiagnosticsMessageCollector(parentMessageCollector), disposable,
                    scriptCompilationConfiguration,
                    scriptCompilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
                ),
                SourceCode::convertToFirViaLightTree
            )
        )
    } finally {
        Disposer.dispose(disposable)
    }
}

fun SourceCode.convertToFirViaLightTree(session: FirSession, diagnosticsReporter: BaseDiagnosticsCollector): FirFile {
    val sourcesToPathsMapper = session.sourcesToPathsMapper
    val builder = LightTree2Fir(session, session.kotlinScopeProvider, diagnosticsReporter)
    val (sanitizedText, linesMapping) = text.byteInputStream(Charsets.UTF_8).use {
        it.reader().readSourceFileWithMapping()
    }
    return builder.buildFirFile(sanitizedText, toKtSourceFile(), linesMapping).also { firFile ->
        (session.firProvider as FirProviderImpl).recordFile(firFile)
        sourcesToPathsMapper.registerFileSource(firFile.source!!, locationId ?: name!!)
    }
}

@SessionConfiguration
private fun K2ScriptingCompilerEnvironmentInternal.getOrCreateSessionForAnnotationResolution(
    scriptCompilationConfiguration: ScriptCompilationConfiguration
): FirSession {
    val dependencies = scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies].toClassPathOrEmpty()
    if (dependencies.isNotEmpty()) {
        configureLibrarySessionIfNeeded(this, compilerContext.environment.configuration, dependencies)
    }
    return dummySessionForAnnotationResolution ?: (createSourceSession(
        moduleDataProvider.addNewScriptModuleData(Name.special("<raw-script>")),
        AbstractProjectFileSearchScope.EMPTY,
        createIncrementalCompilationSymbolProviders = { null },
        extensionRegistrars,
        compilerContext.environment.configuration,
        context = sessionFactoryContext,
        needRegisterJavaElementFinder = true,
        isForLeafHmppModule = false,
        init = {},
    ).apply {
        register(
            FirScriptCompilationComponent::class,
            FirScriptCompilationComponent(hostConfiguration, getSessionForAnnotationResolution = { _, _ -> this })
        )
        dummySessionForAnnotationResolution = this
    })
}

