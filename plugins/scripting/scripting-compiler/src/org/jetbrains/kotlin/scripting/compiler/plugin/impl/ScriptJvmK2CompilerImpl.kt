/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForLightTree
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.ModuleCompilerEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertAnalyzedFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.generateCodeFromIr
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirComposedDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.java.FirSyntheticPropertiesStorage
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.resolve.FirJvmDefaultImportsProvider
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.AbstractFirSpecificAnnotationResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportsProviderHolder
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirLookupDefaultStarImportsInSourcesSettingHolder
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirOverrideService
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirIntersectionOverrideStorage
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.FirSubstitutionOverrideStorage
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirJsSessionFactory.flattenAndFilterOwnProviders
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.fir.session.SourcesToPathsMapper
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.fir.session.registerCommonComponents
import org.jetbrains.kotlin.fir.session.registerJavaComponents
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirFunctionTypeKindService
import org.jetbrains.kotlin.fir.types.FirFunctionTypeKindServiceImpl
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.TypeComponents
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.getOrStoreRefinedCompilationConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.getRefinedOrBaseCompilationConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.scriptRefinedCompilationConfigurationsCache
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.collectScriptsCompilationDependenciesRecursively
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.FirScriptCompilationComponent
import org.jetbrains.kotlin.scripting.resolve.resolvedImportScripts
import org.jetbrains.kotlin.toSourceLinesMapping
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.tryCreateCallableMapping
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.*
import kotlin.script.experimental.impl._languageVersion
import kotlin.script.experimental.impl.refineOnAnnotationsWithLazyDataCollection
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty
import kotlin.script.experimental.jvm.util.toSourceCodePosition

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

class ScriptJvmK2CompilerImpl(
    state: K2ScriptingCompilerEnvironment,
    private val convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile,
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
        scriptCompilationConfiguration.refineAll(script)
            .onSuccess {
                compileImpl(script, it)
            }
    }

    context(reportingCtx: ErrorReportingContext)
    private fun ScriptCompilationConfiguration.refineAll(
        script: SourceCode,
    ): ResultWithDiagnostics<ScriptCompilationConfiguration> = refineBeforeParsing(script)
        .onSuccess {
//            it.refineOnSyntaxTree(script) {
//                ScriptCollectedData(mapOf(ScriptCollectedData.syntaxTree to parseToSyntaxTree(script)))
//            }
//        }.onSuccess {
            it.refineOnAnnotationsWithLazyDataCollection(script) {
                collectScriptAnnotations(script, it)
            }
        }.onSuccess {
            it.refineBeforeCompiling(script)
        }.onSuccess {
            val resolvedScripts = it[ScriptCompilationConfiguration.importScripts]?.map { imported ->
                if (imported is FileBasedScriptSource && !imported.file.exists())
                    return makeFailureResult("Imported source file not found: ${imported.file}".asErrorDiagnostics(path = script.locationId))
                when (imported) {
                    is FileScriptSource -> {
                        val absoluteFile = imported.file.normalize().absoluteFile
                        if (imported.file == absoluteFile) imported else FileScriptSource(absoluteFile)
                    }
                    else -> imported
                }
            }
            if (resolvedScripts.isNullOrEmpty()) it.asSuccess()
            else it.with {
                resolvedImportScripts(resolvedScripts)
            }.asSuccess()
        }.onSuccess {
            it.withUpdatedClasspath(
                state.hostConfiguration[ScriptingHostConfiguration.configurationDependencies].toClassPathOrEmpty()
            ).with {
                _languageVersion(state.compilerContext.environment.configuration.languageVersionSettings.languageVersion.versionString)
            }.asSuccess()
        }

    context(reportingCtx: ErrorReportingContext)
    private fun failure(
        diagnosticsCollector: BaseDiagnosticsCollector,
        vararg diagnostics: ScriptDiagnostic,
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

        val project = state.projectEnvironment.project
        val compilerConfiguration = state.compilerContext.environment.configuration.copy().apply {
            jvmTarget = selectJvmTarget(scriptRefinedCompilationConfiguration, reportingCtx.messageCollector)
        }


        state.hostConfiguration[ScriptingHostConfiguration.scriptRefinedCompilationConfigurationsCache]!!.storeRefinedCompilationConfiguration(
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

        val session = FirJvmSessionFactory.createSourceSession(
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

        session.register(FirScriptCompilationComponent::class, FirScriptCompilationComponent(state.hostConfiguration))

        val sourcesToFir = allSourceFiles.associateWith { it.convertToFir(session, reportingCtx.diagnosticsCollector) }

        if (reportingCtx.diagnosticsCollector.hasErrors) return failure(reportingCtx.diagnosticsCollector)

        checkKotlinPackageUsageForLightTree(compilerConfiguration, sourcesToFir.values, reportingCtx.messageCollector)

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

        val dummySession =
            createDummySessionForScriptRefinement(
                state.baseLibrarySession, script, acceptedAnnotations, state.extensionRegistrars, state.hostConfiguration,
                state.projectEnvironment.getJavaModuleResolver()
            )

        val firFile = script.convertToFir(dummySession, diagnosticsCollector)
        if (diagnosticsCollector.hasErrors)
            return failure(diagnosticsCollector)

        fun loadAnnotation(firAnnotation: FirAnnotation): ResultWithDiagnostics<ScriptSourceAnnotation<Annotation>?> =
            (firAnnotation as? FirAnnotationCall)?.toAnnotationObjectIfMatches(acceptedAnnotations, dummySession, firFile)?.onSuccess {
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
    body: (ScriptJvmK2CompilerImpl) -> T,
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
    disposable: Disposable,
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
        builder.buildFirFile(text, toKtSourceFile(), linesMapping).also { firFile ->
            (session.firProvider as FirProviderImpl).recordFile(firFile)
            sourcesToPathsMapper.registerFileSource(firFile.source!!, locationId ?: name!!)
        }
    }
}

@OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
private fun createDummySessionForScriptRefinement(
    baseLibrarySession: FirSession,
    script: SourceCode,
    acceptedAnnotations: List<KClass<Annotation>>,
    extensionRegistrars: List<FirExtensionRegistrar>,
    hostConfiguration: ScriptingHostConfiguration,
    javaModuleResolver: JavaModuleResolver
): FirSession {

    val annotationsComponent = object : FirAnnotationsPlatformSpecificSupportComponent() {
        override val requiredAnnotationsWithArguments: Set<ClassId> = acceptedAnnotations.mapTo(mutableSetOf()) {
            ClassId.topLevel(FqName.fromSegments(it.qualifiedName!!.split('.')))
        }

        override val requiredAnnotations: Set<ClassId> = requiredAnnotationsWithArguments

        override val volatileAnnotations: Set<ClassId> = setOf(StandardClassIds.Annotations.Volatile)

        override val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean> = mapOf(
            StandardClassIds.Annotations.Deprecated to true,
            StandardClassIds.Annotations.SinceKotlin to true,
        )

        override fun symbolContainsRepeatableAnnotation(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
            return symbol.hasAnnotationWithClassId(StandardClassIds.Annotations.Repeatable, session)
        }

        override fun extractBackingFieldAnnotationsFromProperty(
            property: FirProperty,
            session: FirSession,
            propertyAnnotations: List<FirAnnotation>,
            backingFieldAnnotations: List<FirAnnotation>,
        ): AnnotationsPosition? {
            return null
        }
    }

    return object : FirSession(Kind.Source) {}.apply {
        register(FirCachesFactory::class, FirThreadUnsafeCachesFactory)
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
        registerCommonComponents(languageVersionSettings, false)
        registerJavaComponents(javaModuleResolver)
        register(TypeComponents::class, TypeComponents(this))
        register(InferenceComponents::class, InferenceComponents(this))
        register(FirExtensionService::class, FirExtensionService(this))
        register(FirKotlinScopeProvider::class, FirKotlinScopeProvider())
        register(FirProvider::class, FirProviderImpl(this, kotlinScopeProvider))
        register(SourcesToPathsMapper::class, SourcesToPathsMapper())
        register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotationsImpl(this))
        register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
        register(FirHiddenDeprecationProvider::class, FirHiddenDeprecationProvider(this))
        register(FirLazyDeclarationResolver::class, FirDummyCompilerLazyDeclarationResolver)
        register(FirDefaultImportsProviderHolder.of(FirJvmDefaultImportsProvider))
        register(FirQualifierResolver::class, FirQualifierResolverImpl(this))
        register(FirTypeResolver::class, FirTypeResolverImpl(this))
        register(FirFunctionTypeKindService::class, FirFunctionTypeKindServiceImpl(this))
        register(FirDeclaredMemberScopeProvider::class, FirDeclaredMemberScopeProvider(this))
        register(FirOverrideChecker::class, FirStandardOverrideChecker(this))
        register(FirSubstitutionOverrideStorage::class, FirSubstitutionOverrideStorage(this))
        register(FirIntersectionOverrideStorage::class, FirIntersectionOverrideStorage(this))
        register(FirOverrideService::class, FirOverrideService(this))
        register(FirComposedDiagnosticRendererFactory::class, FirComposedDiagnosticRendererFactory())
        register(FirSyntheticPropertiesStorage::class, FirSyntheticPropertiesStorage(this))

        register(
            FirLookupDefaultStarImportsInSourcesSettingHolder::class,
            FirLookupDefaultStarImportsInSourcesSettingHolder.createDefault(languageVersionSettings)
        )

        register(FirAnnotationsPlatformSpecificSupportComponent::class, annotationsComponent)

        register(FirBuiltinSyntheticFunctionInterfaceProvider::class, baseLibrarySession.syntheticFunctionInterfacesSymbolProvider)

        val providersWithShared = baseLibrarySession.symbolProvider.flattenAndFilterOwnProviders()

        register(FirSymbolProvider::class, FirCachingCompositeSymbolProvider(this, providersWithShared))

        register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, FirEmptySymbolProvider(this))

        FirSessionConfigurator(this).apply {
            for (extensionRegistrar in extensionRegistrars) {
                registerExtensions(extensionRegistrar.configure())
            }
        }.configure()

        register(FirScriptCompilationComponent::class, FirScriptCompilationComponent(hostConfiguration))
    }
}

private class FirScriptSpecificAnnotationResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: CompilerRequiredAnnotationsComputationSession
) : AbstractFirSpecificAnnotationResolveTransformer(session, scopeSession, computationSession) {
    override fun shouldTransformDeclaration(declaration: FirDeclaration): Boolean {
        return true
    }
}


private fun FirAnnotationCall.toAnnotationObjectIfMatches(
    expectedAnnClasses: List<KClass<out Annotation>>,
    session: FirSession,
    firFile: FirFile
): ResultWithDiagnostics<Annotation>? {
    val shortName = when (val typeRef = annotationTypeRef) {
        is FirResolvedTypeRef -> typeRef.coneType.classId?.shortClassName ?: return null
        is FirUserTypeRef -> typeRef.qualifier.last().name
        else -> return null
    }.asString()
    val expectedAnnClass = expectedAnnClasses.firstOrNull { it.simpleName == shortName } ?: return null
    val ctor = expectedAnnClass.constructors.firstOrNull() ?: return null
    val scopeSession = ScopeSession()

    class FirEnumAnnotationArgumentsTransformerDispatcher : FirAbstractBodyResolveTransformerDispatcher(
        session,
        FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS,
        scopeSession = scopeSession,
        implicitTypeOnly = false,
        // This transformer is only used for COMPILER_REQUIRED_ANNOTATIONS, which is <=SUPER_TYPES,
        // so we can't yet expand typealiases.
        expandTypeAliases = false,
        outerBodyResolveContext = null
    ) {
        override val expressionsTransformer: FirExpressionsResolveTransformer = FirExpressionsResolveTransformer(this)
        override val declarationsTransformer: FirDeclarationsResolveTransformer? = null
    }

//    val t = FirScriptSpecificAnnotationResolveTransformer(session, scopeSession, CompilerRequiredAnnotationsComputationSession())

    createImportingScopes(firFile, session, scopeSession)
//    val a = t.withFileScopes(firFile) { t.transformAnnotationCall(this, null) }
    val transformer = FirEnumAnnotationArgumentsTransformerDispatcher().expressionsTransformer
    val a =
        transformer.context.withFile(firFile, transformer.components) {
            withFileAnalysisExceptionWrapping(firFile) {
                transformer.transformAnnotationCall(this, ResolutionMode.ContextDependent)
            }
        }
//    val a = transformer.transformAnnotationCall(this, ResolutionMode.ContextDependent)
    val evalRes = FirExpressionEvaluator.evaluateAnnotationArguments(this, session)
    val mapping =
        tryCreateCallableMapping(
            ctor,
            argumentList.arguments.map {
                when (it) {
                    // TODO: class refs?
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
