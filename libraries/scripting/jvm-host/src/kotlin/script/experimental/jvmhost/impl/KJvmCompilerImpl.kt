/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package kotlin.script.experimental.jvmhost.impl

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.ScriptsCompilationDependencies
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.collectScriptsCompilationDependencies
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getMergedScriptText
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.impl.BridgeDependenciesResolver
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.KotlinJars
import kotlin.script.experimental.jvm.withUpdatedClasspath
import kotlin.script.experimental.jvmhost.KJvmCompilerProxy
import kotlin.script.experimental.util.getOrError

class KJvmCompilerImpl(val hostConfiguration: ScriptingHostConfiguration) : KJvmCompilerProxy {

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val messageCollector = ScriptDiagnosticsMessageCollector()
        val reportingState = ReportingState()

        fun failure(vararg diagnostics: ScriptDiagnostic): ResultWithDiagnostics.Failure =
            ResultWithDiagnostics.Failure(*messageCollector.diagnostics.toTypedArray(), *diagnostics)

        fun failure(message: String): ResultWithDiagnostics.Failure =
            failure(message.asErrorDiagnostics(path = script.locationId))

        val disposable = Disposer.newDisposable()

        try {
            setIdeaIoUseFallback()

            // TODO: refactor/cleanup when the internal resolving API will allow easier info passing between resolver and compiler

            val kotlinCompilerConfiguration =
                createInitialCompilerConfiguration(scriptCompilationConfiguration, messageCollector, reportingState)

            val initialScriptCompilationConfiguration =
                scriptCompilationConfiguration.withUpdatesFromCompilerConfiguration(kotlinCompilerConfiguration)

            val sourcesWithRefinementsState = SourcesWithRefinedConfigurations(script)

            kotlinCompilerConfiguration.add(
                JVMConfigurationKeys.SCRIPT_DEFINITIONS,
                makeScriptDefinition(initialScriptCompilationConfiguration, script, sourcesWithRefinementsState)
            )

            val environment = KotlinCoreEnvironment.createForProduction(
                disposable, kotlinCompilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            val mainKtFile = getMainKtFile(script, initialScriptCompilationConfiguration, environment)
                ?: return failure("Unable to make PSI file from script")
            val ktScript = mainKtFile.declarations.firstIsInstanceOrNull<KtScript>()
                ?: return failure("Not a script file")

            val sourceFiles = arrayListOf(mainKtFile)
            val (classpath, newSources, sourceDependencies) =
                collectScriptsCompilationDependencies(
                    kotlinCompilerConfiguration,
                    environment.project,
                    sourceFiles
                )

            // TODO: consider removing, it is probably redundant: the actual index update is performed with environment.updateClasspath
            kotlinCompilerConfiguration.addJvmClasspathRoots(classpath)
            environment.updateClasspath(classpath.map(::JvmClasspathRoot))

            sourceFiles.addAll(newSources)

            // collectScriptsCompilationDependencies calls resolver for every file, so at this point all updated configurations are collected
            environment.configuration.updateWithRefinedConfigurations(
                initialScriptCompilationConfiguration, sourcesWithRefinementsState.refinedConfigurations, messageCollector, reportingState
            )

            val analysisResult = analyze(sourceFiles, environment)

            if (!analysisResult.shouldGenerateCode) return failure("no code to generate")
            if (analysisResult.isError() || messageCollector.hasErrors()) return failure()

            val generationState = generate(analysisResult, sourceFiles, kotlinCompilerConfiguration)

            val compiledScript =
                makeCompiledScript(generationState, script, ktScript, sourceDependencies) { ktFile ->
                    sourcesWithRefinementsState.refinedConfigurations.entries.find { ktFile.name == it.key.name }?.value
                        ?: initialScriptCompilationConfiguration
                }

            return ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
        } catch (ex: Throwable) {
            return failure(ex.asDiagnostics(path = script.locationId))
        } finally {
            disposable.dispose()
        }
    }

    private class SourcesWithRefinedConfigurations(rootScript: SourceCode) {
        val knownSources = hashSetOf(rootScript)
        val refinedConfigurations = hashMapOf<SourceCode, ScriptCompilationConfiguration>()
    }

    private class ReportingState {
        var currentArguments = K2JVMCompilerArguments()
    }

    private fun makeScriptDefinition(
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        mainScript: SourceCode,
        sourcsesWithConfigurationsState: SourcesWithRefinedConfigurations
    ): BridgeScriptDefinition =
        BridgeScriptDefinition(
            scriptCompilationConfiguration,
            hostConfiguration,
            { script, updatedConfiguration ->
                sourcsesWithConfigurationsState.refinedConfigurations[script] = updatedConfiguration
                updatedConfiguration[ScriptCompilationConfiguration.importScripts]?.let {
                    sourcsesWithConfigurationsState.knownSources.addAll(it)
                }
            },
            { scriptContents ->
                val name = scriptContents.file?.name
                sourcsesWithConfigurationsState.knownSources.find {
                    // TODO: consider using merged text (likely should be cached)
                    // on the other hand it may become obsolete when scripting internals will be redesigned properly
                    (name != null && name == it.scriptFileName(
                        mainScript,
                        scriptCompilationConfiguration
                    )) || it.text == scriptContents.text
                }
            }
        )

    private fun ScriptCompilationConfiguration.withUpdatesFromCompilerConfiguration(kotlinCompilerConfiguration: CompilerConfiguration) =
        withUpdatedClasspath(kotlinCompilerConfiguration.jvmClasspathRoots)

    private fun createInitialCompilerConfiguration(
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        messageCollector: MessageCollector,
        reportingState: ReportingState
    ): CompilerConfiguration {

        val baseArguments = K2JVMCompilerArguments()
        parseCommandLineArguments(
            scriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions] ?: emptyList(),
            baseArguments
        )

        reportArgumentsIgnoredGenerally(baseArguments, messageCollector, reportingState)
        reportingState.currentArguments = baseArguments

        return org.jetbrains.kotlin.config.CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            setupCommonArguments(baseArguments)

            setupJvmSpecificArguments(baseArguments)

            // Default value differs from the argument's default (see #KT-29405 and #KT-29319)
            put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)

            val jdkHomeFromConfigurations = scriptCompilationConfiguration.getNoDefault(ScriptCompilationConfiguration.jvm.jdkHome)
                ?: hostConfiguration[ScriptingHostConfiguration.jvm.jdkHome]
            if (jdkHomeFromConfigurations != null) {
                messageCollector.report(CompilerMessageSeverity.LOGGING, "Using JDK home directory $jdkHomeFromConfigurations")
                put(JVMConfigurationKeys.JDK_HOME, jdkHomeFromConfigurations)
            } else {
                configureJdkHome(baseArguments)
            }

            put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

            val isModularJava = isModularJava()

            scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies]?.let { dependencies ->
                addJvmClasspathRoots(
                    dependencies.flatMap {
                        (it as JvmDependency).classpath
                    }
                )
            }

            add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, ScriptingCompilerConfigurationComponentRegistrar())

            configureExplicitContentRoots(baseArguments)

            if (!baseArguments.noStdlib) {
                addModularRootIfNotNull(isModularJava, "kotlin.stdlib", KotlinJars.stdlib)
                addModularRootIfNotNull(isModularJava, "kotlin.script.runtime", KotlinJars.scriptRuntimeOrNull)
            }
            // see comments about logic in CompilerConfiguration.configureStandardLibs
            if (!baseArguments.noReflect && !baseArguments.noStdlib) {
                addModularRootIfNotNull(isModularJava, "kotlin.reflect", KotlinJars.reflectOrNull)
            }

            put(CommonConfigurationKeys.MODULE_NAME, baseArguments.moduleName ?: "kotlin-script")

            configureAdvancedJvmOptions(baseArguments)
        }
    }

    companion object {

        private fun SourceCode.scriptFileName(
            mainScript: SourceCode,
            scriptCompilationConfiguration: ScriptCompilationConfiguration
        ): String =
            when {
                name != null -> name!!
                mainScript == this -> "script.${scriptCompilationConfiguration[ScriptCompilationConfiguration.fileExtension]}"
                else -> throw Exception("Unexpected script without name: $this")
            }

        private fun getMainKtFile(
            mainScript: SourceCode,
            scriptCompilationConfiguration: ScriptCompilationConfiguration,
            environment: KotlinCoreEnvironment
        ): KtFile? {
            val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl
            val scriptText = getMergedScriptText(mainScript, scriptCompilationConfiguration)
            val virtualFile = ScriptLightVirtualFile(
                mainScript.scriptFileName(mainScript, scriptCompilationConfiguration),
                (mainScript as? FileScriptSource)?.file?.path,
                scriptText
            )
            return psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
        }

        private fun CompilerConfiguration.updateWithRefinedConfigurations(
            initialScriptCompilationConfiguration: ScriptCompilationConfiguration,
            refinedScriptCompilationConfigurations: HashMap<SourceCode, ScriptCompilationConfiguration>,
            messageCollector: ScriptDiagnosticsMessageCollector,
            reportingState: ReportingState
        ) {
            val updatedCompilerOptions = refinedScriptCompilationConfigurations.flatMap {
                it.value[ScriptCompilationConfiguration.compilerOptions] ?: emptyList()
            }
            if (updatedCompilerOptions.isNotEmpty() &&
                updatedCompilerOptions != initialScriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions]
            ) {

                val updatedArguments = K2JVMCompilerArguments()
                parseCommandLineArguments(updatedCompilerOptions, updatedArguments)

                reportArgumentsIgnoredGenerally(updatedArguments, messageCollector, reportingState)
                reportArgumentsIgnoredFromRefinement(updatedArguments, messageCollector, reportingState)

                setupCommonArguments(updatedArguments)

                setupJvmSpecificArguments(updatedArguments)

                configureAdvancedJvmOptions(updatedArguments)
            }
        }

        private fun analyze(sourceFiles: Collection<KtFile>, environment: KotlinCoreEnvironment): AnalysisResult {
            val messageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]!!

            val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector, environment.configuration.languageVersionSettings)

            analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
                val project = environment.project
                TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                    project,
                    sourceFiles,
                    NoScopeRecordCliBindingTrace(),
                    environment.configuration,
                    environment::createPackagePartProvider
                )
            }
            return analyzerWithCompilerReport.analysisResult
        }

        private fun generate(
            analysisResult: AnalysisResult, sourceFiles: List<KtFile>, kotlinCompilerConfiguration: CompilerConfiguration
        ): GenerationState {
            val generationState = GenerationState.Builder(
                sourceFiles.first().project,
                ClassBuilderFactories.BINARIES,
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                sourceFiles,
                kotlinCompilerConfiguration
            ).build()

            KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)
            return generationState
        }

        private fun makeCompiledScript(
            generationState: GenerationState,
            script: SourceCode,
            ktScript: KtScript,
            sourceDependencies: List<ScriptsCompilationDependencies.SourceDependencies>,
            getScriptConfiguration: (KtFile) -> ScriptCompilationConfiguration
        ): KJvmCompiledScript<Any> {
            val scriptDependenciesStack = ArrayDeque<KtScript>()

            fun makeOtherScripts(script: KtScript): List<KJvmCompiledScript<*>> {

                // TODO: ensure that it is caught earlier (as well) since it would be more economical
                if (scriptDependenciesStack.contains(script))
                    throw IllegalArgumentException("Unable to handle recursive script dependencies")
                scriptDependenciesStack.push(script)

                val containingKtFile = script.containingKtFile
                val otherScripts: List<KJvmCompiledScript<*>> =
                    sourceDependencies.find { it.scriptFile == containingKtFile }?.sourceDependencies?.mapNotNull { sourceFile ->
                        sourceFile.declarations.firstIsInstanceOrNull<KtScript>()?.let {
                            KJvmCompiledScript<Any>(
                                containingKtFile.virtualFile?.path,
                                getScriptConfiguration(sourceFile),
                                it.fqName.asString(),
                                makeOtherScripts(it)
                            )
                        }
                    } ?: emptyList()

                scriptDependenciesStack.pop()
                return otherScripts
            }

            return KJvmCompiledScript(
                script.locationId,
                getScriptConfiguration(ktScript.containingKtFile),
                ktScript.fqName.asString(),
                makeOtherScripts(ktScript),
                KJvmCompiledModule(generationState)
            )
        }

        private fun reportArgumentsIgnoredGenerally(
            arguments: K2JVMCompilerArguments,
            messageCollector: MessageCollector,
            reportingState: ReportingState
        ) {

            reportIgnoredArguments(
                arguments,
                "The following compiler arguments are ignored on script compilation: ",
                messageCollector, 
                reportingState,
                K2JVMCompilerArguments::version,
                K2JVMCompilerArguments::destination,
                K2JVMCompilerArguments::buildFile,
                K2JVMCompilerArguments::commonSources,
                K2JVMCompilerArguments::allWarningsAsErrors,
                K2JVMCompilerArguments::script,
                K2JVMCompilerArguments::scriptTemplates,
                K2JVMCompilerArguments::scriptResolverEnvironment,
                K2JVMCompilerArguments::disableStandardScript,
                K2JVMCompilerArguments::disableDefaultScriptingPlugin,
                K2JVMCompilerArguments::pluginClasspaths,
                K2JVMCompilerArguments::pluginOptions,
                K2JVMCompilerArguments::useJavac,
                K2JVMCompilerArguments::compileJava,
                K2JVMCompilerArguments::reportPerf,
                K2JVMCompilerArguments::dumpPerf
            )
        }

        private fun reportArgumentsIgnoredFromRefinement(
            arguments: K2JVMCompilerArguments, messageCollector: MessageCollector, reportingState: ReportingState
        ) {
            reportIgnoredArguments(
                arguments,
                "The following compiler arguments are ignored when configured from refinement callbacks: ",
                messageCollector, 
                reportingState,
                K2JVMCompilerArguments::noJdk,
                K2JVMCompilerArguments::jdkHome,
                K2JVMCompilerArguments::javaModulePath,
                K2JVMCompilerArguments::classpath,
                K2JVMCompilerArguments::noStdlib,
                K2JVMCompilerArguments::noReflect
            )
        }

        private fun reportIgnoredArguments(
            arguments: K2JVMCompilerArguments, message: String,
            messageCollector: MessageCollector, reportingState: ReportingState,
            vararg toIgnore: KMutableProperty1<K2JVMCompilerArguments, *>
        ) {
            val ignoredArgKeys = toIgnore.mapNotNull { argProperty ->
                if (argProperty.get(arguments) != argProperty.get(reportingState.currentArguments)) {
                    argProperty.findAnnotation<Argument>()?.value
                        ?: throw IllegalStateException("unknown compiler argument property: $argProperty: no Argument annotation found")
                } else null
            }

            if (ignoredArgKeys.isNotEmpty()) {
                messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, "$message${ignoredArgKeys.joinToString(", ")}")
            }
        }
    }
}

internal class ScriptDiagnosticsMessageCollector : MessageCollector {

    private val _diagnostics = arrayListOf<ScriptDiagnostic>()

    val diagnostics: List<ScriptDiagnostic> get() = _diagnostics

    override fun clear() {
        _diagnostics.clear()
    }

    override fun hasErrors(): Boolean =
        _diagnostics.any { it.severity == ScriptDiagnostic.Severity.ERROR }


    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        val mappedSeverity = when (severity) {
            CompilerMessageSeverity.EXCEPTION,
            CompilerMessageSeverity.ERROR -> ScriptDiagnostic.Severity.ERROR
            CompilerMessageSeverity.STRONG_WARNING,
            CompilerMessageSeverity.WARNING -> ScriptDiagnostic.Severity.WARNING
            CompilerMessageSeverity.INFO -> ScriptDiagnostic.Severity.INFO
            CompilerMessageSeverity.LOGGING -> ScriptDiagnostic.Severity.DEBUG
            else -> null
        }
        if (mappedSeverity != null) {
            val mappedLocation = location?.let {
                if (it.line < 0 && it.column < 0) null // special location created by CompilerMessageLocation.create
                else SourceCode.Location(SourceCode.Position(it.line, it.column))
            }
            _diagnostics.add(ScriptDiagnostic(message, mappedSeverity, location?.path, mappedLocation))
        }
    }
}

// A bridge to the current scripting
// mostly copies functionality from KotlinScriptDefinitionAdapterFromNewAPI[Base]
// reusing it requires structural changes that doesn't seem justified now, since the internals of the scripting should be reworked soon anyway
// TODO: either finish refactoring of the scripting internals or reuse KotlinScriptDefinitionAdapterFromNewAPI[BAse] here
internal class BridgeScriptDefinition(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val hostConfiguration: ScriptingHostConfiguration,
    updateConfiguration: (SourceCode, ScriptCompilationConfiguration) -> Unit,
    getScriptSource: (ScriptContents) -> SourceCode?
) : KotlinScriptDefinition(Any::class) {

    val baseClass: KClass<*> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        getScriptingClass(scriptCompilationConfiguration.getOrError(ScriptCompilationConfiguration.baseClass))
    }

    override val template: KClass<*> get() = baseClass

    override val name: String
        get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.displayName] ?: "Kotlin Script"

    override val fileType: LanguageFileType = KotlinFileType.INSTANCE

    override fun isScript(fileName: String): Boolean =
        fileName.endsWith(".$fileExtension")

    override fun getScriptName(script: KtScript): Name {
        val fileBasedName = NameUtils.getScriptNameForFile(script.containingKtFile.name)
        return Name.identifier(fileBasedName.identifier.removeSuffix(".$fileExtension"))
    }

    override val fileExtension: String
        get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.fileExtension] ?: super.fileExtension

    override val acceptedAnnotations = run {
        val cl = this::class.java.classLoader
        scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.annotations
            ?.map { (cl.loadClass(it.typeName) as Class<out Annotation>).kotlin }
            ?: emptyList()
    }

    override val implicitReceivers: List<KType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scriptCompilationConfiguration[ScriptCompilationConfiguration.implicitReceivers]
            .orEmpty()
            .map { getScriptingClass(it).starProjectedType }
    }

    override val providedProperties: List<Pair<String, KType>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scriptCompilationConfiguration[ScriptCompilationConfiguration.providedProperties]
            ?.map { (k, v) -> k to getScriptingClass(v).starProjectedType }.orEmpty()
    }

    override val additionalCompilerArguments: List<String>
        get() = scriptCompilationConfiguration[ScriptCompilationConfiguration.compilerOptions]
            .orEmpty()

    override val dependencyResolver: DependenciesResolver =
        BridgeDependenciesResolver(scriptCompilationConfiguration, updateConfiguration, getScriptSource)

    private val scriptingClassGetter by lazy(LazyThreadSafetyMode.PUBLICATION) {
        hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
            ?: throw IllegalArgumentException("Expecting 'getScriptingClass' property in the scripting environment")
    }

    private fun getScriptingClass(type: KotlinType) =
        scriptingClassGetter(
            type,
            KotlinScriptDefinition::class, // Assuming that the KotlinScriptDefinition class is loaded in the proper classloader
            hostConfiguration
        )
}

internal class ScriptLightVirtualFile(name: String, private val _path: String?, text: String) :
    LightVirtualFile(name, KotlinLanguage.INSTANCE, StringUtil.convertLineSeparators(text)) {

    init {
        charset = CharsetToolkit.UTF8_CHARSET
    }

    override fun getPath(): String = _path ?: super.getPath()
    override fun getCanonicalPath(): String? = path
}