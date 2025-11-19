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
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.services.scriptDefinitionProviderService
import org.jetbrains.kotlin.toSourceLinesMapping
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.tryCreateCallableMapping
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.impl._languageVersion
import kotlin.script.experimental.impl.refineOnAnnotationsWithLazyDataCollection
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.util.toSourceCodePosition

class ScriptJvmK2CompilerIsolated(val hostConfiguration: ScriptingHostConfiguration) : ScriptCompilerProxy {
    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript> =
        withK2ScriptCompilerWithLightTree(
            scriptCompilationConfiguration.with {
                hostConfiguration(this@ScriptJvmK2CompilerIsolated.hostConfiguration)
            }
        ) {
            it.compile(script)
        }
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
            DiagnosticReporterFactory.createPendingReporter(),
            state.compilerContext.environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )
    ) {
        scriptCompilationConfiguration.refineBeforeParsing(script)
            .onSuccess {
                it.refineOnAnnotationsWithLazyDataCollection(script) {
                    collectScriptAnnotations(script, it)
                }
            }.onSuccess {
                it.refineBeforeCompiling(script)
            }.onSuccess {
                compileImpl(
                    script,
                    it.with {
                        _languageVersion(state.compilerContext.environment.configuration.languageVersionSettings.languageVersion.versionString)
                    }
                )
            }
    }

    context(reportingCtx: ErrorReportingContext)
    private fun failure(
        diagnosticsCollector: BaseDiagnosticsCollector,
        vararg diagnostics: ScriptDiagnostic
    ): ResultWithDiagnostics.Failure {
        diagnosticsCollector.reportToMessageCollector(reportingCtx.messageCollector, reportingCtx.renderDiagnosticName)
        return ResultWithDiagnostics.Failure(*reportingCtx.messageCollector.diagnostics.toTypedArray<ScriptDiagnostic>(), *diagnostics)
    }

    @OptIn(LegacyK2CliPipeline::class, DirectDeclarationsAccess::class)
    context(reportingCtx: ErrorReportingContext)
    private fun compileImpl(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript> {

        val project = state.projectEnvironment.project
        val compilerConfiguration = state.compilerContext.environment.configuration.copy().apply {
            jvmTarget = selectJvmTarget(scriptCompilationConfiguration, reportingCtx.messageCollector)
        }

        val extensionRegistrars = FirExtensionRegistrar.getInstances(project)
        val compilerEnvironment = ModuleCompilerEnvironment(state.projectEnvironment, reportingCtx.diagnosticsCollector)
        val renderDiagnosticName = compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        val targetId = TargetId(script.name ?: "main", "java-production")

        scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies]?.let { dependencies ->
            val classpathFiles = dependencies.flatMap {
                (it as? JvmDependency)?.classpath ?: emptyList()
            }
            // needed for class finders for now anyway
            compilerConfiguration.addJvmClasspathRoots(classpathFiles)
            state.compilerContext.environment.updateClasspath(classpathFiles.map(::JvmClasspathRoot))
            val (libModuleData, _) = state.moduleDataProvider.addNewLibraryModuleDataIfNeeded(classpathFiles.map(File::toPath))
            if (libModuleData != null) {
                val projectEnvironment = state.sessionFactoryContext.projectEnvironment
                val searchScope = state.moduleDataProvider.getModuleDataPaths(libModuleData)?.let { paths ->
                    projectEnvironment.getSearchScopeByClassPath(paths)
                } ?: state.sessionFactoryContext.librariesScope

                createScriptingAdditionalLibrariesSession(
                    libModuleData,
                    state.sessionFactoryContext,
                    state.moduleDataProvider,
                    state.sharedLibrarySession,
                    extensionRegistrars,
                    compilerConfiguration,
                    getKotlinClassFinder = { projectEnvironment.getKotlinClassFinder(searchScope) },
                    getJavaFacade = { projectEnvironment.getFirJavaFacade(it, libModuleData, state.sessionFactoryContext.librariesScope) }
                )
            }
        }

        val moduleData = state.moduleDataProvider.addNewScriptModuleData(Name.special("<script-${script.name ?: "main"}>"))

        val session = FirJvmSessionFactory.createSourceSession(
            moduleData,
            AbstractProjectFileSearchScope.EMPTY,
            createIncrementalCompilationSymbolProviders = { null },
            extensionRegistrars,
            compilerConfiguration,
            context = state.sessionFactoryContext,
            needRegisterJavaElementFinder = true,
            isForLeafHmppModule = false,
            init = {},
        )

        session.scriptDefinitionProviderService!!.storeRefinedConfiguration(script, scriptCompilationConfiguration.asSuccess())

        val rawFir = script.convertToFir(session, reportingCtx.diagnosticsCollector)

        val outputs = listOf(resolveAndCheckFir(session, listOf(rawFir), reportingCtx.diagnosticsCollector)).also {
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
                rawFir.declarations.firstIsInstanceOrNull<FirScript>()
                    ?.let { it.symbol.packageFqName().child(NameUtils.getScriptTargetClassName(it.name)) }
            },
            emptyList(),
            { scriptCompilationConfiguration },
            extractResultFields(irInput.irModuleFragment)
        ).onSuccess { compiledScript ->
            ResultWithDiagnostics.Success(compiledScript, reportingCtx.messageCollector.diagnostics)
        }
    }

    context(reportingCtx: ErrorReportingContext)
    private fun collectScriptAnnotations(
        script: SourceCode,
        compilationConfiguration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<ScriptCollectedData> {
        val hostConfiguration =
            compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
        val contextClassLoader = hostConfiguration[ScriptingHostConfiguration.jvm.baseClassLoader]
        val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
        val jvmGetScriptingClass = (getScriptingClass as? GetScriptingClassByClassLoader)
            ?: throw IllegalArgumentException("Expecting class implementing GetScriptingClassByClassLoader in the hostConfiguration[getScriptingClass], got $getScriptingClass")
        val acceptedAnnotations =
            compilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.flatMap {
                it.annotations.mapNotNull { ann ->
                    @Suppress("UNCHECKED_CAST")
                    jvmGetScriptingClass(ann, contextClassLoader, hostConfiguration) as? KClass<Annotation> // TODO errors
                }
            }?.takeIf { it.isNotEmpty() } ?: return ScriptCollectedData(emptyMap()).asSuccess()
        // separate reporter for refinement to avoid double raw fir warnings reporting
        val diagnosticsCollector = DiagnosticReporterFactory.createPendingReporter()
        val firFile = script.convertToFir(
            createDummySessionForScriptRefinement(script),
            diagnosticsCollector
        )
        if (diagnosticsCollector.hasErrors)
            return failure(diagnosticsCollector)

        fun loadAnnotation(firAnnotation: FirAnnotation): ResultWithDiagnostics<ScriptSourceAnnotation<Annotation>?> =
            (firAnnotation as? FirAnnotationCall)?.toAnnotationObjectIfMatches(acceptedAnnotations)?.onSuccess {
                val location = script.locationId
                val startPosition = firAnnotation.source?.startOffset?.toSourceCodePosition(script)
                val endPosition = firAnnotation.source?.endOffset?.toSourceCodePosition(script)
                ScriptSourceAnnotation(
                    it,
                    if (location != null && startPosition != null)
                        SourceCode.LocationWithId(
                            location, SourceCode.Location(startPosition, endPosition)
                        )
                    else null
                ).asSuccess()
            } ?: ResultWithDiagnostics.Success(null)

        return firFile.annotations.mapNotNullSuccess(::loadAnnotation).onSuccess { annotations ->
            ScriptCollectedData(mapOf(ScriptCollectedData.collectedAnnotations to annotations)).asSuccess()
        }
    }
}

fun <T> withK2ScriptCompilerWithLightTree(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    parentMessageCollector: MessageCollector? = null,
    moduleName: Name = Name.special("<script-module>"),
    body: (ScriptJvmK2CompilerImpl) -> T
): T {
    val disposable = Disposer.newDisposable("Default disposable for scripting compiler")
    return try {
        body(createK2ScriptCompilerWithLightTree(scriptCompilationConfiguration, parentMessageCollector, moduleName, disposable))
    } finally {
        Disposer.dispose(disposable)
    }
}

fun createK2ScriptCompilerWithLightTree(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    parentMessageCollector: MessageCollector? = null,
    moduleName: Name = Name.special("<script-module>"),
    disposable: Disposable
): ScriptJvmK2CompilerImpl {
    val state =
        createCompilerState(
            moduleName, ScriptDiagnosticsMessageCollector(parentMessageCollector), disposable,
            scriptCompilationConfiguration,
            scriptCompilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
        )
    return ScriptJvmK2CompilerImpl(state) { session, diagnosticsReporter ->
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
        register(
            FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(
                LanguageVersionSettingsImpl.DEFAULT,
                isMetadataCompilation = false
            )
        )
        register(FirExtensionService::class, FirExtensionService(this))
        register(FirKotlinScopeProvider::class, FirKotlinScopeProvider())
        register(FirProvider::class, FirProviderImpl(this, kotlinScopeProvider))
        register(SourcesToPathsMapper::class, SourcesToPathsMapper())
    }

private fun FirAnnotationCall.toAnnotationObjectIfMatches(expectedAnnClasses: List<KClass<out Annotation>>): ResultWithDiagnostics<Annotation>? {
    val shortName = when (val typeRef = annotationTypeRef) {
        is FirResolvedTypeRef -> typeRef.coneType.classId?.shortClassName ?: return null
        is FirUserTypeRef -> typeRef.qualifier.last().name
        else -> return null
    }.asString()
    val expectedAnnClass = expectedAnnClasses.firstOrNull { it.simpleName == shortName } ?: return null
    val ctor = expectedAnnClass.constructors.firstOrNull() ?: return null
    val mapping =
        tryCreateCallableMapping(
            ctor,
            argumentList.arguments.map {
                when (it) {
                    // TODO: classrefs?
                    is FirLiteralExpression -> it.value
                    else -> null
                }
            }
        )
    return if (mapping != null)
        try {
            ctor.callBy(mapping).asSuccess()
        } catch (e: Error) {
            makeFailureResult(e.asDiagnostics())
        }
    else null
}
