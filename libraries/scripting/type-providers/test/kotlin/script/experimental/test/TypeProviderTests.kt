/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import com.intellij.openapi.Disposable
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.typeProviders.AnnotationBasedTypeProvider
import kotlin.script.experimental.typeProviders.typeProviders

abstract class TypeProviderTests(
    protected val testDataPath: String
) : TestCase() {
    private val testRootDisposable: Disposable = TestDisposable()

    protected fun fail(failure: ResultWithDiagnostics.Failure): Nothing = kotlin.test.fail(
        failure.reports.joinToString("\n") { it.message }
    )

    protected inline fun <reified A : Annotation> expectCompilerErrors(
        scriptPath: String,
        typeProvider: AnnotationBasedTypeProvider<A>
    ): List<ScriptDiagnostic> = when (val result = compileScript(File(testDataPath, scriptPath).toScriptSource(), typeProvider)) {
        is ResultWithDiagnostics.Success -> kotlin.test.fail("Script at $scriptPath successfully compiled. It was expected to fail")
        is ResultWithDiagnostics.Failure -> result.reports
    }

    // Running

    protected inline fun <reified A : Annotation> runScriptOrFail(
        scriptPath: String,
        typeProvider: AnnotationBasedTypeProvider<A>
    ): String = runScript(scriptPath, typeProvider).valueOr {
        fail(it)
    }

    protected inline fun <reified A : Annotation> runScript(
        scriptPath: String,
        typeProvider: AnnotationBasedTypeProvider<A>
    ): ResultWithDiagnostics<String> {
        return runScript(scriptPath, typeProvider, A::class)
    }

    protected fun <A : Annotation> runScript(
        scriptPath: String,
        typeProvider: AnnotationBasedTypeProvider<A>,
        annotationType: KClass<A>
    ): ResultWithDiagnostics<String> {
        val source = File(testDataPath, scriptPath).toScriptSource()
        return compileScript(source, typeProvider, annotationType)
            .onSuccess { compiled ->
                captureOut {
                    val evaluator = BasicJvmScriptEvaluator()
                    val result = runBlocking {
                        evaluator(compiled)
                    }

                    result
                }.asSuccess()
            }
    }

    // Compilation

    protected inline fun <reified A : Annotation> compileScript(
        script: SourceCode,
        typeProvider: AnnotationBasedTypeProvider<A>
    ) = compileScript(script, typeProvider, A::class)

    protected fun <A : Annotation> compileScript(
        script: SourceCode,
        typeProvider: AnnotationBasedTypeProvider<A>,
        annotationType: KClass<A>,
    ): ResultWithDiagnostics<CompiledScript> {
        val scriptCompilationConfiguration = ScriptCompilationConfiguration {
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
            typeProviders {
                add(typeProvider, annotationType)
            }
        }

        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.FULL_JDK).apply {
            val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration)
            add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromConfigurations(
                    hostConfiguration,
                    scriptCompilationConfiguration,
                    ScriptEvaluationConfiguration.Default
                )
            )
            loadScriptingPlugin(this)
        }

        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val scriptCompiler = ScriptJvmCompilerFromEnvironment(environment)

        return scriptCompiler.compile(script, scriptCompilationConfiguration)
    }

}

internal fun captureOutResultWithDiagnostics(body: () -> ResultWithDiagnostics<*>): ResultWithDiagnostics<String> {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    val result: ResultWithDiagnostics<*> = try {
        body()
    } catch (ex: Throwable) {
        makeFailureResult(ex.asDiagnostics())
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return result.onSuccess { outStream.toString().asSuccess() }
}

internal fun captureOut(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString()
}

internal class TestDisposable : Disposable {
    @Volatile
    var isDisposed = false
        private set

    override fun dispose() {
        isDisposed = true
    }
}