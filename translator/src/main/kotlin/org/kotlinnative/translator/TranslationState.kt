package org.kotlinnative.translator

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
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
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import java.util.*

class TranslationState(val environment: KotlinCoreEnvironment, val bindingContext: BindingContext, arm: Boolean) {
    companion object {
        var pointerAlign = 4
        var pointerSize = 4
    }

    init {
        pointerAlign = if (arm) 4 else 8
        pointerSize = if (arm) 4 else 8
    }

    var externalFunctions = HashMap<String, FunctionCodegen>()
    var functions = HashMap<String, FunctionCodegen>()
    val globalVariableCollection = HashMap<String, LLVMVariable>()
    var classes = HashMap<String, ClassCodegen>()
    var objects = HashMap<String, ObjectCodegen>()
    var properties = HashMap<String, PropertyCodegen>()
    val codeBuilder = LLVMBuilder(arm)
    val mainFunctions = ArrayList<String>()

    val extensionFunctions = HashMap<String, HashMap<String, FunctionCodegen>>()

}

fun parseAndAnalyze(sources: List<String>, disposer: Disposable, arm: Boolean = false): TranslationState {

    val configuration = CompilerConfiguration()
    val messageCollector = object : MessageCollector {
        private var hasError = false

        override fun hasErrors(): Boolean = hasError

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            if (!severity.isError) {
                return
            }

            System.err.println("[${severity.toString()}]${location.path} ${location.line}:${location.column} $message")
            hasError = true
        }
    }

    configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    configuration.put(JVMConfigurationKeys.MODULE_NAME, JvmAbi.DEFAULT_MODULE_NAME)

    configuration.addKotlinSourceRoots(sources)

    val environment = KotlinCoreEnvironment.createForProduction(disposer, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    val bindingContext = analyze(environment)?.bindingContext ?: throw TranslationException()

    return TranslationState(environment, bindingContext, arm)
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
            System.err.println(runtimes.joinToString { it.toString() })
        }
    })

    return if (analyzer.hasErrors()) null else analyzer.analysisResult
}
