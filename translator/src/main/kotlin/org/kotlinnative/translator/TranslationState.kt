package org.kotlinnative.translator

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.getModuleName
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.utils.PathUtil
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.utils.FunctionDescriptor
import java.util.*


class TranslationState(sources: List<String>, disposer: Disposable) {

    val environment: KotlinCoreEnvironment
    val bindingContext: BindingContext
    var functions = HashMap<String, FunctionDescriptor>()
    val variableManager = VariableManager()

    init {
        val configuration = CompilerConfiguration()
        val messageCollector = GroupingMessageCollector(object : MessageCollector {
            private var hasError = false

            override fun hasErrors(): Boolean {
                return hasError
            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
                println("[report] $message")
                hasError = severity.isError || hasError
            }
        })

        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        configuration.put(JVMConfigurationKeys.MODULE_NAME, JvmAbi.DEFAULT_MODULE_NAME)

        configuration.addKotlinSourceRoots(sources)

        environment = KotlinCoreEnvironment.createForProduction(disposer, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        bindingContext = analyze(environment)?.bindingContext ?: throw TranslationException()
    }

    fun analyze(environment: KotlinCoreEnvironment): AnalysisResult? {
        val collector = environment.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        val analyzer = AnalyzerWithCompilerReport(collector)
        analyzer.analyzeAndReport(environment.getSourceFiles(), object : AnalyzerWithCompilerReport.Analyzer {
            override fun analyze(): AnalysisResult {
                val sharedTrace = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()
                val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.getModuleName())

                return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                        moduleContext,
                        environment.getSourceFiles(),
                        sharedTrace,
                        environment.configuration.get(JVMConfigurationKeys.MODULES),
                        environment.configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
                        JvmPackagePartProvider(environment))
            }

            override fun reportEnvironmentErrors() {
                val files = environment.configuration.jvmClasspathRoots
                val runtimes = files.map { it.canonicalFile }.filter { it.name == PathUtil.KOTLIN_JAVA_RUNTIME_JAR && it.exists() }
                collector.report(CompilerMessageSeverity.ERROR, runtimes.joinToString { it.path }, CompilerMessageLocation.NO_LOCATION)
                println(runtimes.joinToString { it.toString() })
            }
        })

        return if (analyzer.hasErrors()) null else analyzer.analysisResult
    }

}