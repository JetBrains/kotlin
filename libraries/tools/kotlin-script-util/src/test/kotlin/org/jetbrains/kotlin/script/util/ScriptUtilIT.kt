/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.PathUtil.getResourcePathForClass
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URI
import java.util.jar.Manifest
import kotlin.reflect.KClass

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
        val scriptClass = compileScript("args-hello-world.kts", StandardScript::class)
        Assert.assertNotNull(scriptClass)
        val ctor = scriptClass?.getConstructor(Array<String>::class.java)
        Assert.assertNotNull(ctor)
        captureOut {
            ctor!!.newInstance(arrayOf("a1"))
        }.let {
            Assert.assertEquals(argsHelloWorldOutput, it)
        }
    }

    @Test
    fun testBndHelloWorld() {
        val scriptClass = compileScript("bindings-hello-world.kts", ScriptWithBindings::class)
        Assert.assertNotNull(scriptClass)
        val ctor = scriptClass?.getConstructor(Map::class.java)
        Assert.assertNotNull(ctor)
        captureOut {
            ctor!!.newInstance(hashMapOf("a1" to 42))
        }.let {
            Assert.assertEquals(bindingsHelloWorldOutput, it)
        }
    }

    @Test
    fun testResolveStdHelloWorld() {
        Assert.assertNull(compileScript("args-junit-hello-world.kts", StandardScript::class))

        val scriptClass = compileScript("args-junit-hello-world.kts", StandardScriptWithAnnotatedResolving::class)
        if (scriptClass == null) {
            val resolver = DefaultKotlinAnnotatedScriptDependenciesResolver()
            System.err.println(resolver.baseClassPath)
        }
        Assert.assertNotNull(scriptClass)
        captureOut {
            scriptClass!!.getConstructor(Array<String>::class.java)!!.newInstance(arrayOf("a1"))
        }.let {
            Assert.assertEquals(argsHelloWorldOutput, it)
        }
    }

    private fun compileScript(
            scriptFileName: String,
            scriptTemplate: KClass<out Any>,
            environment: Map<String, Any?>? = null,
            suppressOutput: Boolean = false): Class<*>? =
            compileScriptImpl("src/test/resources/scripts/" + scriptFileName, KotlinScriptDefinitionFromAnnotatedTemplate(scriptTemplate, null, null, environment), suppressOutput)

    private fun compileScriptImpl(
            scriptPath: String,
            scriptDefinition: KotlinScriptDefinition,
            suppressOutput: Boolean): Class<*>?
    {
        val paths = PathUtil.getKotlinPathsForDistDirectory()
        val messageCollector =
                if (suppressOutput) MessageCollector.NONE
                else PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        val rootDisposable = Disposer.newDisposable()
        try {
            val configuration = CompilerConfiguration().apply {
                addJvmClasspathRoots(PathUtil.getJdkClassesRoots())
                val rtJar = System.getProperty("kotlin.java.runtime.jar")
                Assert.assertNotNull(rtJar)
                addJvmClasspathRoot(File(rtJar))
                put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
                addKotlinSourceRoot(scriptPath)
                getResourcePathForClass(DependsOn::class.java).let {
                    if (it.exists()) {
                        addJvmClasspathRoot(it)
                    }
                    else {
                        // attempt to workaround some maven quirks
                        addJvmClasspathRoots(
                                Thread.currentThread().contextClassLoader.manifestClassPath().toList())
                    }
                }
                put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script-util-test")
                add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, scriptDefinition)
                put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
            }

            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            try {
                return KotlinToJVMBytecodeCompiler.compileScript(environment, paths)
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

    private fun ClassLoader.manifestClassPath() =
            getResources("META-INF/MANIFEST.MF")
                    .asSequence()
                    .mapNotNull { ifFailed(null) { it.openStream().use { Manifest().apply { read(it) } } } }
                    .flatMap { it.mainAttributes?.getValue("Class-Path")?.splitToSequence(" ") ?: emptySequence() }
                    .mapNotNull { ifFailed(null) { File(URI.create(it)) } }

    private inline fun <R> ifFailed(default: R, block: () -> R) = try {
        block()
    } catch (t: Throwable) {
        default
    }

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
