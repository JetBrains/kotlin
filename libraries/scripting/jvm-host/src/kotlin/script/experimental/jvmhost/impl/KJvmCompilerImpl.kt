/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
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
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
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
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.util.KotlinJars
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getMergedScriptText
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.impl.BridgeDependenciesResolver
import kotlin.script.experimental.jvm.javaHome
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
                (updatedConfiguration.getNoDefault(ScriptCompilationConfiguration.jvm.javaHome)
                    ?: hostConfiguration[ScriptingHostConfiguration.jvm.javaHome])?.let {
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
            val scriptText = getMergedScriptText(script, updatedConfiguration)
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
                ClassBuilderFactories.BINARIES,
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

            val res = KJvmCompiledScript<Any>(updatedConfiguration, generationState, scriptFileName.capitalize())

            return ResultWithDiagnostics.Success(res, messageCollector.diagnostics)
        } catch (ex: Throwable) {
            return failure(ex.asDiagnostics())
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
                SourceCode.Location(SourceCode.Position(it.line, it.column))
            }
            _diagnostics.add(ScriptDiagnostic(message, mappedSeverity, mappedLocation))
        }
    }
}

// A bridge to the current scripting

internal class BridgeScriptDefinition(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    updateClasspath: (List<File>) -> Unit
) : KotlinScriptDefinition(
    hostConfiguration.getScriptingClass(
        scriptCompilationConfiguration.getOrError(ScriptCompilationConfiguration.baseClass),
        BridgeScriptDefinition::class
    )
) {
    override val acceptedAnnotations = run {
        val cl = this::class.java.classLoader
        scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.annotations
            ?.map { (cl.loadClass(it.typeName) as Class<out Annotation>).kotlin }
            ?: emptyList()
    }

    override val dependencyResolver: DependenciesResolver =
        BridgeDependenciesResolver(scriptCompilationConfiguration, updateClasspath)
}
