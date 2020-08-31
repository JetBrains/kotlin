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
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.typeProviders.AnnotationBasedTypeProvider
import kotlin.script.experimental.typeProviders.generatedCode.*
import kotlin.script.experimental.typeProviders.typeProviders

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class DummyTestAnnotation

abstract class GeneratedCodeTestsBase : TestCase() {
    private val testRootDisposable: Disposable = TestDisposable()

    protected fun runScriptOrFail(
        generatedCode: GeneratedCode.Builder.() -> Unit
    ): String = runScript(generatedCode).valueOr {
        fail(it)
    }

    private fun fail(failure: ResultWithDiagnostics.Failure): Nothing = kotlin.test.fail(
        failure.reports.joinToString("\n") { it.message }
    )

    private fun runScript(
        generatedCode: GeneratedCode.Builder.() -> Unit
    ): ResultWithDiagnostics<String> {
        return compileScript(generatedCode)
            .onSuccess { compiled ->
                captureOutResultWithDiagnostics {
                    val evaluator = BasicJvmScriptEvaluator()
                    runBlocking {
                        evaluator(compiled)
                    }
                }
            }
    }

    private fun compileScript(
        generatedCode: GeneratedCode.Builder.() -> Unit
    ): ResultWithDiagnostics<CompiledScript> {
        // TODO: Refactor to only using `provide` directly.
        //  But for some reason using provide from the start goes on a loop ¯\_(ツ)_/¯
        val typeProvider = object : AnnotationBasedTypeProvider<DummyTestAnnotation> {
            override fun invoke(
                collectedAnnotations: List<ScriptSourceAnnotation<DummyTestAnnotation>>,
                context: AnnotationBasedTypeProvider.Context
            ): ResultWithDiagnostics<GeneratedCode> {
                return GeneratedCode(generatedCode).asSuccess()
            }
        }

        val scriptCompilationConfiguration = ScriptCompilationConfiguration {
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
            typeProviders {
                +typeProvider
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

        val dummyScript = File("libraries/scripting/type-providers/testData/dummy.kts").toScriptSource()
        return scriptCompiler.compile(dummyScript, scriptCompilationConfiguration)
    }
}
