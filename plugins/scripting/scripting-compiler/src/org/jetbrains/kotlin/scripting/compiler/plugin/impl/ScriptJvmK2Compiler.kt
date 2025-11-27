/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.ModuleCompilerEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertAnalyzedFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.generateCodeFromIr
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.flattenAndFilterOwnProviders
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.registerLibrarySessionComponents
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.services.scriptDefinitionProviderService
import org.jetbrains.kotlin.toSourceLinesMapping
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ScriptJvmK2Compiler(
    state: K2ScriptingCompilerEnvironment,
    private val convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile
) : ScriptCompilerProxy {

    private val state = (state as? K2ScriptingCompilerEnvironmentInternal) ?: error("Expected the state of type K2ScriptingCompilerEnvironmentInternal, got ${state::class}")

    fun compile(script: SourceCode) = compile(script, state.baseScriptCompilationConfiguration)

    @OptIn(LegacyK2CliPipeline::class, DirectDeclarationsAccess::class)
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

        fun failure(diagnosticsCollector: BaseDiagnosticsCollector): ResultWithDiagnostics.Failure {
            diagnosticsCollector.reportToMessageCollector(messageCollector, renderDiagnosticName)
            return failure(messageCollector)
        }

        return scriptCompilationConfiguration.refineBeforeParsing(script).onSuccess {
            val collectedData by lazy(LazyThreadSafetyMode.NONE) {
                ScriptCollectedData(
                    mapOf(
                        ScriptCollectedData.fir to listOf(
                            script.convertToFir(
                                createDummySessionForScriptRefinement(script),
                                diagnosticsCollector
                            )
                        )
                    )
                )
            }
            if (diagnosticsCollector.hasErrors) {
                failure(diagnosticsCollector)
            } else {
                it.refineOnFir(script, collectedData)
            }
        }.onSuccess {
            it.refineBeforeCompiling(script)
        }.onSuccess { refinedConfiguration ->
            // TODO: separate reporter for refinement, to avoid double warnings reporting
            // imports -> compile one by one or all here?
            // add all deps

            refinedConfiguration[ScriptCompilationConfiguration.dependencies]?.let { dependencies ->
                val classpathFiles = dependencies.flatMap {
                    (it as? JvmDependency)?.classpath ?: emptyList()
                }
                // likely needed for class finders anyway
                compilerConfiguration.addJvmClasspathRoots(classpathFiles)
                val (libModuleData, _) = state.moduleDataProvider.addNewLibraryModuleDataIfNeeded(classpathFiles.map(File::toPath))
                if (libModuleData != null) {
                    createScriptDependenciesSession(libModuleData,
                                                    state as K2ScriptingCompilerEnvironmentImpl, extensionRegistrars, compilerConfiguration)
                }
            }

            val moduleData = state.moduleDataProvider.addNewScriptModuleData(Name.special("<script-${script.name!!}>"))

            // create source session (with dependent one if 1.1
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

            session.scriptDefinitionProviderService!!.storeRefinedConfiguration(script, refinedConfiguration.asSuccess())

            val rawFir = script.convertToFir(session, diagnosticsCollector)

            val outputs = listOf(resolveAndCheckFir(session, listOf(rawFir), diagnosticsCollector)).also {
                it.runPlatformCheckers(diagnosticsCollector)
            }
            val frontendOutput = AllModulesFrontendOutput(outputs)

            if (diagnosticsCollector.hasErrors) return failure(diagnosticsCollector)

            val irInput = convertAnalyzedFirToIr(compilerConfiguration, targetId, frontendOutput, compilerEnvironment)

            if (diagnosticsCollector.hasErrors) return failure(diagnosticsCollector)

            val generationState = generateCodeFromIr(irInput, compilerEnvironment)

            diagnosticsCollector.reportToMessageCollector(messageCollector, renderDiagnosticName)

            if (diagnosticsCollector.hasErrors) {
                return failure(messageCollector)
            }

            return makeCompiledScript(
                generationState,
                script,
                { rawFir.declarations.firstIsInstanceOrNull<FirScript>()?.let { it.symbol.packageFqName().child(NameUtils.getScriptTargetClassName(it.name)) } },
                emptyList(),
                { refinedConfiguration },
                extractResultFields(irInput.irModuleFragment)
            ).onSuccess { compiledScript ->
                ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
            }
        }
    }
}

fun <T> withK2ScriptCompilerProxyWithLightTree(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    parentMessageCollector: MessageCollector? = null,
    moduleName: Name = Name.special("<script-module>"),
    body: (ScriptJvmK2Compiler) -> T
): T {
    val disposable = Disposer.newDisposable("Default disposable for scripting compiler")
    return try {
        body(createK2ScriptCompilerProxyWithLightTree(scriptCompilationConfiguration, parentMessageCollector, moduleName, disposable))
    } finally {
        Disposer.dispose(disposable)
    }
}

fun createK2ScriptCompilerProxyWithLightTree(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    parentMessageCollector: MessageCollector? = null,
    moduleName: Name = Name.special("<script-module>"),
    disposable: Disposable
): ScriptJvmK2Compiler {
    val state =
        createCompilerState(
            moduleName, ScriptDiagnosticsMessageCollector(parentMessageCollector), disposable,
            scriptCompilationConfiguration,
            scriptCompilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
        )
    return ScriptJvmK2Compiler(state) { session, diagnosticsReporter ->
        val sourcesToPathsMapper = session.sourcesToPathsMapper
        val builder = LightTree2Fir(session, session.kotlinScopeProvider, diagnosticsReporter)
        val linesMapping = text.toSourceLinesMapping()
        builder.buildFirFile(text, toKtSourceFile()!!, linesMapping).also { firFile ->
            (session.firProvider as FirProviderImpl).recordFile(firFile)
            sourcesToPathsMapper.registerFileSource(firFile.source!!, locationId ?: name!!)
        }
    }
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
        register(FirKotlinScopeProvider::class, FirKotlinScopeProvider())
        register(FirProvider::class, FirProviderImpl(this, kotlinScopeProvider))
        register(SourcesToPathsMapper::class, SourcesToPathsMapper())
    }

@OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
private fun createScriptDependenciesSession(
    libModuleData: FirModuleData,
    state: K2ScriptingCompilerEnvironmentImpl,
    extensionRegistrars: List<FirExtensionRegistrar>,
    compilerConfiguration: CompilerConfiguration,
) {
    FirCliSession(FirSession.Kind.Library).apply session@{
        libModuleData.bindSession(this@session)

        registerCliCompilerAndCommonComponents(compilerConfiguration.languageVersionSettings, false)
        registerLibrarySessionComponents(state.sessionFactoryContext)
        register(FirBuiltinSyntheticFunctionInterfaceProvider::class, state.sharedLibrarySession.syntheticFunctionInterfacesSymbolProvider)

        val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        register(FirKotlinScopeProvider::class, kotlinScopeProvider)

        FirSessionConfigurator(this).apply {
            for (extensionRegistrar in extensionRegistrars) {
                registerExtensions(extensionRegistrar.configure())
            }
        }.configure()
        registerCommonComponentsAfterExtensionsAreConfigured()

        val providers = buildList {
            val projectEnvironment = state.sessionFactoryContext.projectEnvironment
            val searchScope = state.moduleDataProvider.getModuleDataPaths(libModuleData)?.let { paths ->
                projectEnvironment.getSearchScopeByClassPath(paths)
            } ?: state.sessionFactoryContext.librariesScope
            val kotlinClassFinder1 = projectEnvironment.getKotlinClassFinder(searchScope)
            add(
                JvmClassFileBasedSymbolProvider(
                    this@session,
                    state.moduleDataProvider,
                    kotlinScopeProvider,
                    state.sessionFactoryContext.packagePartProviderForLibraries,
                    kotlinClassFinder1,
                    projectEnvironment.getFirJavaFacade(this@session, libModuleData, state.sessionFactoryContext.librariesScope),
                )
            )
        }
        register(
            StructuredProviders::class,
            StructuredProviders(
                sourceProviders = emptyList(),
                dependencyProviders = providers,
                sharedProvider = state.sharedLibrarySession.symbolProvider,
            )
        )

        val providersWithShared = providers + state.sharedLibrarySession.symbolProvider.flattenAndFilterOwnProviders()

        val symbolProvider = FirCachingCompositeSymbolProvider(this, providersWithShared)
        register(FirSymbolProvider::class, symbolProvider)
        register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
    }
}
