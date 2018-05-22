/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package kotlin.script.experimental.jvmhost.impl

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.util.KotlinJars
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.host.getMergedScriptText
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.JvmScriptCompileConfigurationProperties
import kotlin.script.experimental.jvm.JvmScriptEvaluationEnvironmentProperties
import kotlin.script.experimental.jvm.KJVMCompilerProxy
import kotlin.script.experimental.jvm.impl.BridgeDependenciesResolver

class KJVMCompiledScript<out ScriptBase : Any>(
    override val configuration: ScriptCompileConfiguration,
    val generationState: GenerationState,
    val scriptClassFQName: String
) : CompiledScript<ScriptBase> {

    override suspend fun instantiate(scriptEvaluationEnvironment: ScriptEvaluationEnvironment): ResultWithDiagnostics<ScriptBase> = try {
        val baseClassLoader = scriptEvaluationEnvironment.getOrNull(JvmScriptEvaluationEnvironmentProperties.baseClassLoader)
        val dependencies = configuration.getOrNull(ScriptCompileConfigurationProperties.dependencies)
            ?.flatMap { (it as? JvmDependency)?.classpath?.map { it.toURI().toURL() } ?: emptyList() }
        // TODO: previous dependencies and classloaders should be taken into account here
        val classLoaderWithDeps =
            if (dependencies == null) baseClassLoader
            else URLClassLoader(dependencies.toTypedArray(), baseClassLoader)
        val classLoader = GeneratedClassLoader(generationState.factory, classLoaderWithDeps)

        val clazz = classLoader.loadClass(scriptClassFQName)
        (clazz as? ScriptBase)?.asSuccess()
                ?: ResultWithDiagnostics.Failure("Compiled class expected to be a subclass of the <ScriptBase>, but got ${clazz.javaClass.name}".asErrorDiagnostics())
    } catch (e: Throwable) {
        ResultWithDiagnostics.Failure(ScriptDiagnostic("Unable to instantiate class $scriptClassFQName", exception = e))
    }
}

class KJVMCompilerImpl : KJVMCompilerProxy {

    override fun compile(
        script: ScriptSource,
        configurator: ScriptCompilationConfigurator?,
        additionalConfiguration: ScriptCompileConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val messageCollector = ScriptDiagnosticsMessageCollector()

        fun failure(vararg diagnostics: ScriptDiagnostic): ResultWithDiagnostics.Failure =
            ResultWithDiagnostics.Failure(*messageCollector.diagnostics.toTypedArray(), *diagnostics)

        try {
            var environment: KotlinCoreEnvironment? = null
            var updatedScriptCompileConfiguration = additionalConfiguration

            fun updateClasspath(classpath: List<File>) {
                environment!!.updateClasspath(classpath.map(::JvmClasspathRoot))
                val updatedDeps = updatedScriptCompileConfiguration.getOrNull(ScriptCompileConfigurationProperties.dependencies)?.plus(
                    JvmDependency(classpath)
                ) ?: listOf(JvmDependency(classpath))
                updatedScriptCompileConfiguration = ScriptCompileConfiguration(
                    updatedScriptCompileConfiguration,
                    ScriptCompileConfigurationProperties.dependencies to updatedDeps
                )
            }

            val disposable = Disposer.newDisposable()
            val kotlinCompilerConfiguration = org.jetbrains.kotlin.config.CompilerConfiguration().apply {
                add(
                    JVMConfigurationKeys.SCRIPT_DEFINITIONS,
                    BridgeScriptDefinition(additionalConfiguration, configurator, ::updateClasspath)
                )
                put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
                put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

                var isModularJava = false
                additionalConfiguration.getOrNull(JvmScriptCompileConfigurationProperties.javaHomeDir)?.let {
                    put(JVMConfigurationKeys.JDK_HOME, it)
                    isModularJava = CoreJrtFileSystem.isModularJdk(it)
                }

                additionalConfiguration.getOrNull(ScriptCompileConfigurationProperties.dependencies)?.let {
                    addJvmClasspathRoots(
                        it.flatMap {
                            (it as JvmDependency).classpath
                        }
                    )
                }
                fun addRoot(moduleName: String, file: File) {
                    if (isModularJava) {
                        add(JVMConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                        add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, moduleName)
                    } else {
                        add(JVMConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(file))
                    }
                }
                // TODO: implement logic similar to compiler's  -no-stdlib (and -no-reflect?)
                addRoot("kotlin.stdlib", KotlinJars.stdlib)

                put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script") // TODO" take meaningful and valid name from somewhere
                languageVersionSettings = LanguageVersionSettingsImpl(
                    LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE, mapOf(AnalysisFlag.skipMetadataVersionCheck to true)
                )
            }
            environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                kotlinCompilerConfiguration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector, environment.configuration.languageVersionSettings)

            val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl
            val scriptText = getMergedScriptText(script, additionalConfiguration)
            val scriptFileName = "script" // TODO: extract from file/url if available
            val virtualFile = LightVirtualFile(
                "$scriptFileName${KotlinParserDefinition.STD_SCRIPT_EXT}",
                KotlinLanguage.INSTANCE,
                StringUtil.convertLineSeparators(scriptText)
            ).apply {
                charset = CharsetToolkit.UTF8_CHARSET
            }
            val psiFile: KtFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                    ?: return failure("Unable to make PSI file from script".asErrorDiagnostics())

            val sourceFiles = listOf(psiFile)

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

            if (!analysisResult.shouldGenerateCode) return failure("no code to generate".asErrorDiagnostics())
            if (analysisResult.isError() || messageCollector.hasErrors()) return failure()

            val generationState = GenerationState.Builder(
                psiFile.project,
                ClassBuilderFactories.binaries(false),
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                sourceFiles,
                kotlinCompilerConfiguration
            ).build()
            generationState.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                generationState,
                psiFile.script!!.containingKtFile.packageFqName,
                setOf(psiFile.script!!.containingKtFile),
                org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION
            )

            val res = KJVMCompiledScript<Any>(updatedScriptCompileConfiguration, generationState, scriptFileName.capitalize())

            return ResultWithDiagnostics.Success(res, messageCollector.diagnostics)
        } catch (ex: Throwable) {
            return failure(ex.asDiagnostics())
        }
    }
}

class ScriptDiagnosticsMessageCollector : MessageCollector {

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
                ScriptSource.Location(ScriptSource.Position(it.line, it.column))
            }
            _diagnostics.add(ScriptDiagnostic(message, mappedSeverity, mappedLocation))
        }
    }
}

// A bridge to the current scripting

internal class BridgeScriptDefinition(
    scriptCompilerConfiguration: ScriptCompileConfiguration,
    scriptConfigurator: ScriptCompilationConfigurator?,
    updateClasspath: (List<File>) -> Unit
) : KotlinScriptDefinition(scriptCompilerConfiguration.getScriptBaseClass(BridgeScriptDefinition::class)) {
    override val acceptedAnnotations = run {
        val cl = this::class.java.classLoader
        scriptCompilerConfiguration.getOrNull(ScriptCompileConfigurationProperties.refineConfigurationOnAnnotations)
            ?.map { (cl.loadClass(it.typeName) as Class<out Annotation>).kotlin }
                ?: emptyList()
    }

    override val dependencyResolver: DependenciesResolver =
        BridgeDependenciesResolver(scriptConfigurator, scriptCompilerConfiguration, updateClasspath)
}

