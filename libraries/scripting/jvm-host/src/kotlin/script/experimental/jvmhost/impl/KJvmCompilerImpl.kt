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
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.util.KotlinJars
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
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
import kotlin.script.experimental.jvmhost.KJvmCompilerProxy
import kotlin.script.experimental.util.getOrError

class KJvmCompilerImpl(val hostConfiguration: ScriptingHostConfiguration) : KJvmCompilerProxy {

    override fun compile(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val messageCollector = ScriptDiagnosticsMessageCollector()

        fun failure(vararg diagnostics: ScriptDiagnostic): ResultWithDiagnostics.Failure =
            ResultWithDiagnostics.Failure(*messageCollector.diagnostics.toTypedArray(), *diagnostics)

        fun failure(message: String): ResultWithDiagnostics.Failure =
            ResultWithDiagnostics.Failure(
                *messageCollector.diagnostics.toTypedArray(),
                message.asErrorDiagnostics(path = script.locationId)
            )

        try {
            setIdeaIoUseFallback()

            var environment: KotlinCoreEnvironment? = null
            var updatedConfiguration = scriptCompilationConfiguration

            fun updateClasspath(classpath: List<File>) {
                environment!!.updateClasspath(classpath.map(::JvmClasspathRoot))
                if (classpath.isNotEmpty()) {
                    updatedConfiguration = ScriptCompilationConfiguration(updatedConfiguration) {
                        dependencies.append(JvmDependency(classpath))
                    }
                }
            }

            val disposable = Disposer.newDisposable()
            val kotlinCompilerConfiguration = org.jetbrains.kotlin.config.CompilerConfiguration().apply {
                add(
                    JVMConfigurationKeys.SCRIPT_DEFINITIONS,
                    BridgeScriptDefinition(scriptCompilationConfiguration, hostConfiguration, ::updateClasspath)
                )
                put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
                put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

                var isModularJava = false
                (updatedConfiguration.getNoDefault(ScriptCompilationConfiguration.jvm.jdkHome)
                    ?: hostConfiguration[ScriptingHostConfiguration.jvm.jdkHome])?.let {
                    put(JVMConfigurationKeys.JDK_HOME, it)
                    isModularJava = CoreJrtFileSystem.isModularJdk(it)
                }

                updatedConfiguration[ScriptCompilationConfiguration.dependencies]?.let { dependencies ->
                    addJvmClasspathRoots(
                        dependencies.flatMap {
                            (it as JvmDependency).classpath
                        }
                    )
                }
                fun addRoot(moduleName: String, file: File) {
                    if (isModularJava) {
                        add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                        add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, moduleName)
                    } else {
                        add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(file))
                    }
                }
                // TODO: implement logic similar to compiler's  -no-stdlib (and -no-reflect?)
                addRoot("kotlin.stdlib", KotlinJars.stdlib)
                KotlinJars.scriptRuntimeOrNull?.let { addRoot("kotlin.script.runtime", it) }

                put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script") // TODO" take meaningful and valid name from somewhere
                languageVersionSettings = LanguageVersionSettingsImpl(
                    LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE, mapOf(AnalysisFlags.skipMetadataVersionCheck to true)
                )
            }
            environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                kotlinCompilerConfiguration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector, environment.configuration.languageVersionSettings)

            val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl
            val scriptText = getMergedScriptText(script, updatedConfiguration)
            val scriptFileName = script.name ?: "script.${updatedConfiguration[ScriptCompilationConfiguration.fileExtension]}"

            val virtualFile = ScriptLightVirtualFile(scriptFileName, (script as? FileScriptSource)?.file?.path, scriptText)

            val psiFile: KtFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                ?: return failure("Unable to make PSI file from script")

            val ktScript = psiFile.declarations.firstIsInstanceOrNull<KtScript>()
                ?: return failure("Not a script file")

            val sourceFiles = arrayListOf(psiFile)
            val (classpath, newSources, sourceDependencies) =
                collectScriptsCompilationDependencies(kotlinCompilerConfiguration, environment.project, sourceFiles)
            kotlinCompilerConfiguration.addJvmClasspathRoots(classpath)
            sourceFiles.addAll(newSources)

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
            val analysisResult = analyzerWithCompilerReport.analysisResult

            if (!analysisResult.shouldGenerateCode) return failure("no code to generate")
            if (analysisResult.isError() || messageCollector.hasErrors()) return failure()

            val generationState = GenerationState.Builder(
                psiFile.project,
                ClassBuilderFactories.BINARIES,
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                sourceFiles,
                kotlinCompilerConfiguration
            ).build()

            KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)

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
                                containingKtFile.virtualFile?.path, updatedConfiguration, it.fqName.asString(), makeOtherScripts(it)
                            )
                        }
                    } ?: emptyList()

                scriptDependenciesStack.pop()
                return otherScripts
            }

            val compiledScript = KJvmCompiledScript<Any>(
                script.locationId,
                updatedConfiguration,
                ktScript.fqName.asString(),
                makeOtherScripts(ktScript),
                KJvmCompiledModule(generationState)
            )

            return ResultWithDiagnostics.Success(compiledScript, messageCollector.diagnostics)
        } catch (ex: Throwable) {
            return failure(ex.asDiagnostics(path = script.locationId))
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
    updateClasspath: (List<File>) -> Unit
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
        BridgeDependenciesResolver(scriptCompilationConfiguration, updateClasspath)

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