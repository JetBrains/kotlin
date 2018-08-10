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
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.util.templates.BindingsScriptTemplateWithLocalResolving
import org.jetbrains.kotlin.script.util.templates.StandardArgsScriptTemplateWithIvyResolving
import org.jetbrains.kotlin.script.util.templates.StandardArgsScriptTemplateWithLocalResolving
import org.jetbrains.kotlin.script.util.templates.StandardArgsScriptTemplateWithMavenResolving
import org.jetbrains.kotlin.utils.PathUtil.getResourcePathForClass
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import kotlin.reflect.KClass

const val KOTLIN_JAVA_RUNTIME_JAR = "kotlin-stdlib.jar"

class ScriptUtilIT {

    companion object {
        private val argsHelloWorldOutput =
"""Hello, world!
a1
done
"""
        private val bindingsHelloWorldOutput =
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

    @Test
    fun testResolveStdJUnitHelloWorld() {
        val savedErr = System.err
        try {
            System.setErr(PrintStream(NullOutputStream()))
            Assert.assertNull(compileScript("args-junit-hello-world.kts", StandardArgsScriptTemplateWithLocalResolving::class))
        }
        finally {
            System.setErr(savedErr)
        }

        val scriptClass = compileScript("args-junit-hello-world.kts", StandardArgsScriptTemplateWithMavenResolving::class)
        if (scriptClass == null) {
            System.err.println(classpathFromClassloader(Thread.currentThread().contextClassLoader)?.takeIfContainsAll(KOTLIN_JAVA_RUNTIME_JAR)?.joinToString())
        }
        Assert.assertNotNull(scriptClass)
        captureOut {
            scriptClass!!.getConstructor(Array<String>::class.java)!!.newInstance(arrayOf("a1"))
        }.let {
            Assert.assertEquals(argsHelloWorldOutput.linesSplitTrim(), it.linesSplitTrim())
        }
    }

    @Test
    fun testIvyResolveStdJUnitHelloWorld() {
        val scriptClass = compileScript("args-junit-hello-world.kts", StandardArgsScriptTemplateWithIvyResolving::class)
        if (scriptClass == null) {
            System.err.println(classpathFromClassloader(Thread.currentThread().contextClassLoader)?.takeIfContainsAll(KOTLIN_JAVA_RUNTIME_JAR)?.joinToString())
        }
        Assert.assertNotNull(scriptClass)
        captureOut {
            scriptClass!!.getConstructor(Array<String>::class.java)!!.newInstance(arrayOf("a1"))
        }.let {
            Assert.assertEquals(argsHelloWorldOutput.linesSplitTrim(), it.linesSplitTrim())
        }
    }

    private fun compileScript(
            scriptFileName: String,
            scriptTemplate: KClass<out Any>,
            environment: Map<String, Any?>? = null,
            suppressOutput: Boolean = false
    ): Class<*>? =
            compileScriptImpl("libraries/tools/kotlin-script-util/src/test/resources/scripts/" + scriptFileName,
                              KotlinScriptDefinitionFromAnnotatedTemplate(scriptTemplate, environment), suppressOutput)

    private fun compileScriptImpl(
            scriptPath: String,
            scriptDefinition: KotlinScriptDefinition,
            suppressOutput: Boolean
    ): Class<*>? {
        val messageCollector =
                if (suppressOutput) MessageCollector.NONE
                else PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        val rootDisposable = Disposer.newDisposable()
        try {
            val configuration = CompilerConfiguration().apply {
                classpathFromClassloader(Thread.currentThread().contextClassLoader)?.takeIfContainsAll(KOTLIN_JAVA_RUNTIME_JAR)?.let {
                    addJvmClasspathRoots(it)
                }

                put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
                addKotlinSourceRoot(scriptPath)
                getResourcePathForClass(DependsOn::class.java).let {
                    if (it.exists()) {
                        addJvmClasspathRoot(it)
                    }
                }
                put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script-util-test")
                add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, scriptDefinition)
                put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
            }

            val environment = KotlinCoreEnvironment.createForTests(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            try {
                return KotlinToJVMBytecodeCompiler.compileScript(environment)
            }
            catch (e: CompilationException) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                        MessageUtil.psiElementToMessageLocation(e.element))
                return null
            }
            catch (t: Throwable) {
                MessageCollectorUtil.reportException(messageCollector, t)
                throw t
            }

        }
        finally {
            Disposer.dispose(rootDisposable)
        }
    }

    private fun String.linesSplitTrim() =
            split('\n','\r').map(String::trim).filter(String::isNotBlank)

    private fun captureOut(body: () -> Unit): String {
        val outStream = ByteArrayOutputStream()
        val prevOut = System.out
        System.setOut(PrintStream(outStream))
        try {
            body()
        }
        finally {
            System.out.flush()
            System.setOut(prevOut)
        }
        return outStream.toString()
    }
}

private class NullOutputStream : OutputStream() {
    override fun write(b: Int) { }
    override fun write(b: ByteArray) { }
    override fun write(b: ByteArray, off: Int, len: Int) { }
}

