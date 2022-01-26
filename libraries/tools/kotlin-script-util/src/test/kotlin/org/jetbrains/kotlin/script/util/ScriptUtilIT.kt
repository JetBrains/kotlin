/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script.util

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.script.util.templates.BindingsScriptTemplateWithLocalResolving
import org.jetbrains.kotlin.script.util.templates.StandardArgsScriptTemplateWithLocalResolving
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.toCompilerMessageSeverity
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.PathUtil.getResourcePathForClass
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.reflect.KClass
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext

const val KOTLIN_JAVA_RUNTIME_JAR = "kotlin-stdlib.jar"

class ScriptUtilIT {

    companion object {
        private const val argsHelloWorldOutput =
            """Hello, world!
a1
done
"""
        private const val bindingsHelloWorldOutput =
            """Hello, world!
a1 = 42
done
"""
    }

    @Test
    fun testArgsHelloWorld() {
        val scriptClass = compileScript("args-hello-world.kts", StandardArgsScriptTemplateWithLocalResolving::class)
        Assert.assertNotNull(scriptClass)
        val ctor = scriptClass?.getConstructor(Array<String>::class.java)
        Assert.assertNotNull(ctor)
        captureOut {
            ctor!!.newInstance(arrayOf("a1"))
        }.let {
            Assert.assertEquals(argsHelloWorldOutput.linesSplitTrim(), it.linesSplitTrim())
        }
    }

    @Test
    fun testBndHelloWorld() {
        val scriptClass = compileScript("bindings-hello-world.kts", BindingsScriptTemplateWithLocalResolving::class)
        Assert.assertNotNull(scriptClass)
        val ctor = scriptClass?.getConstructor(Map::class.java)
        Assert.assertNotNull(ctor)
        captureOut {
            ctor!!.newInstance(hashMapOf("a1" to 42))
        }.let {
            Assert.assertEquals(bindingsHelloWorldOutput.linesSplitTrim(), it.linesSplitTrim())
        }
    }

    private fun compileScript(
        scriptFileName: String,
        scriptTemplate: KClass<out Any>,
        environment: Map<String, Any?>? = null,
        suppressOutput: Boolean = false
    ): Class<*>? =
        compileScriptImpl(
            "libraries/tools/kotlin-script-util/src/test/resources/scripts/$scriptFileName",
            KotlinScriptDefinitionFromAnnotatedTemplate(
                scriptTemplate,
                environment
            ), suppressOutput
        )

    private fun compileScriptImpl(
        scriptPath: String,
        scriptDefinition: KotlinScriptDefinition,
        suppressOutput: Boolean
    ): Class<*>? {
        val messageCollector =
            if (suppressOutput) MessageCollector.NONE
            else PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, true)

        val rootDisposable = Disposer.newDisposable()
        try {
            val configuration = CompilerConfiguration().apply {
                addJvmClasspathRoots(scriptCompilationClasspathFromContext(KOTLIN_JAVA_RUNTIME_JAR))
                configureJdkClasspathRoots()

                put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
                addKotlinSourceRoot(scriptPath)
                @Suppress("DEPRECATION")
                getResourcePathForClass(DependsOn::class.java).let {
                    if (it.exists()) {
                        addJvmClasspathRoot(it)
                    }
                }
                put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script-util-test")
                add(
                    ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                    ScriptDefinition.FromLegacy(
                        defaultJvmScriptingHostConfiguration, scriptDefinition
                    )
                )
                put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

                add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, ScriptingCompilerConfigurationComponentRegistrar())
            }

            val environment = KotlinCoreEnvironment.createForTests(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            environment.getSourceFiles().forEach {
                messageCollector.report(CompilerMessageSeverity.LOGGING, "file: $it -> script def: ${it.findScriptDefinition()?.name}")
            }
            messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                "compilation classpath:\n  ${environment.configuration.jvmClasspathRoots.joinToString("\n  ")}"
            )

            val scriptCompiler = ScriptJvmCompilerFromEnvironment(environment)

            return try {
                val script = File(scriptPath).toScriptSource()
                val newScriptDefinition = ScriptDefinitionProvider.getInstance(environment.project)!!.findDefinition(script)!!
                val compiledScript = scriptCompiler.compile(script, newScriptDefinition.compilationConfiguration).onSuccess {
                    @Suppress("DEPRECATION_ERROR")
                    internalScriptingRunSuspend {
                        it.getClass(newScriptDefinition.evaluationConfiguration)
                    }
                }.valueOr {
                    for (report in it.reports) {
                        messageCollector.report(report.severity.toCompilerMessageSeverity(), report.render(withSeverity = false))
                    }
                    return null
                }
                compiledScript.java
            } catch (e: CompilationException) {
                messageCollector.report(
                    CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                    MessageUtil.psiElementToMessageLocation(e.element)
                )
                null
            } catch (e: IllegalStateException) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e))
                null
            } catch (t: Throwable) {
                MessageCollectorUtil.reportException(messageCollector, t)
                throw t
            }

        } finally {
            Disposer.dispose(rootDisposable)
        }
    }

    private fun String.linesSplitTrim() =
        split('\n', '\r').map(String::trim).filter(String::isNotBlank)

    private fun captureOut(body: () -> Unit): String = captureOutAndErr(body).first

    private fun captureOutAndErr(body: () -> Unit): Pair<String, String> {
        val outStream = ByteArrayOutputStream()
        val errStream = ByteArrayOutputStream()
        val prevOut = System.out
        val prevErr = System.err
        System.setOut(PrintStream(outStream))
        System.setErr(PrintStream(errStream))
        try {
            body()
        } finally {
            System.out.flush()
            System.err.flush()
            System.setOut(prevOut)
            System.setErr(prevErr)
        }
        return outStream.toString() to errStream.toString()
    }
}
