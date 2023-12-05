/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.ThrowableRunnable
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.TestDisposable
import org.jetbrains.kotlin.scripting.compiler.plugin.TestMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.updateWithBaseCompilerArguments
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.RunAll
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.*
import kotlin.script.experimental.jvm.*

private const val testDataPath = "plugins/scripting/scripting-compiler/testData/cliCompilation"

class ScriptCliCompilationTest : TestCase() {
    private val testRootDisposable: Disposable = TestDisposable("${ScriptCliCompilationTest::class.simpleName}.testRootDisposable")

    override fun tearDown() {
        RunAll(
            ThrowableRunnable { Disposer.dispose(testRootDisposable) },
            ThrowableRunnable { super.tearDown() },
        )
    }

    fun testPrerequisites() {
        Assert.assertTrue(thisClasspath.isNotEmpty())
    }

    fun testSimpleScript() {
        val out = checkRun("hello.kts")
        Assert.assertEquals("Hello from basic script!", out)
    }

    fun testEmptyScript() {
        val emptyFile = Files.createTempFile("empty",".kts").toFile()
        try {
            Assert.assertTrue(
                "Script file is not empty",
                emptyFile.exists() && emptyFile.isFile && emptyFile.length() == 0L
            )
            checkRun(emptyFile)
        } finally {
            emptyFile.delete()
        }
    }

    fun testSimpleScriptWithArgs() {
        val out = checkRun("hello_args.kts", listOf("kotlin"))
        Assert.assertEquals("Hello, kotlin!", out)
    }

    fun testScriptWithRequire() {
        val out = checkRun("hello.req1.kts", scriptDef = TestScriptWithRequire::class)
        Assert.assertEquals("Hello from required!", out)
    }


    private val thisClasspath = listOf(PathUtil.getResourcePathForClass(ScriptCliCompilationTest::class.java))

    private fun runCompiler(
        script: File,
        args: List<String> = emptyList(),
        scriptDef: KClass<*>? = null,
        classpath: List<File> = emptyList()
    ): Pair<ExitCode, MessageCollector> {

        val collector = TestMessageCollector()

        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.FULL_JDK).apply {
            updateWithBaseCompilerArguments()
            put(MESSAGE_COLLECTOR_KEY, collector)
            if (scriptDef != null) {
                val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                    configurationDependencies(JvmDependency(classpath))
                }
                add(
                    ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                    ScriptDefinition.FromTemplate(hostConfiguration, scriptDef, ScriptDefinition::class)
                )
            }
            loadScriptingPlugin(this)
        }

        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        return compileAndExecuteScript(script.toScriptSource(), environment, null, args) to collector
    }

    private fun checkRun(
        scriptFileName: String,
        args: List<String> = emptyList(),
        scriptDef: KClass<*>? = null,
        classpath: List<File> = emptyList()
    ): String = checkRun(File(testDataPath, scriptFileName), args, scriptDef, classpath)

    private fun checkRun(
        scriptFile: File,
        args: List<String> = emptyList(),
        scriptDef: KClass<*>? = null,
        classpath: List<File> = emptyList()
    ): String =
        captureOut {
            val res = runCompiler(scriptFile, args, scriptDef, classpath)
            val resMessage = lazy {
                "Compilation results:\n" + res.second.toString()
            }
            Assert.assertEquals(resMessage.value, ExitCode.OK, res.first)
            Assert.assertFalse(resMessage.value, res.second.hasErrors())
        }
}

@KotlinScript(
    fileExtension = "req1.kts",
    compilationConfiguration = TestScriptWithRequireConfiguration::class
)
abstract class TestScriptWithRequire

object TestScriptWithRequireConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(Import::class, DependsOn::class)
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
        refineConfiguration {
            onAnnotations(Import::class, DependsOn::class) { context: ScriptConfigurationRefinementContext ->
                val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
                val sources = context.collectedData?.get(ScriptCollectedData.foundAnnotations)
                    ?.flatMap {
                        (it as? Import)?.sources?.map { sourceName ->
                            FileScriptSource(scriptBaseDir?.resolve(sourceName) ?: File(sourceName))
                        } ?: emptyList()
                    }
                val deps = context.collectedData?.get(ScriptCollectedData.foundAnnotations)
                    ?.mapNotNull {
                        (it as? DependsOn)?.path?.let(::File)
                    }
                ScriptCompilationConfiguration(context.compilationConfiguration) {
                    if (sources?.isNotEmpty() == true) importScripts.append(sources)
                    if (deps != null) updateClasspath(deps)
                }.asSuccess()
            }
        }
    }
)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Import(vararg val sources: String)
