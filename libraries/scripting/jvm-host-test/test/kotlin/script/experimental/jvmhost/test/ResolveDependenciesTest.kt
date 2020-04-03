/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import org.junit.Test
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.util.classpathFromClass
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ResolveDependenciesTest : TestCase() {

    private val configurationWithDependenciesFromClassloader = ScriptCompilationConfiguration {
        dependencies(JvmDependencyFromClassLoader { ShouldBeVisibleFromScript::class.java.classLoader })
    }

    private val configurationWithDependenciesFromClasspath = ScriptCompilationConfiguration {
        updateClasspath(classpathFromClass(ShouldBeVisibleFromScript::class))
    }

    private val thisPackage = ShouldBeVisibleFromScript::class.java.`package`.name

    private val classAccessScript = "${thisPackage}.ShouldBeVisibleFromScript().x".toScriptSource()
    private val classImportScript = "import ${thisPackage}.ShouldBeVisibleFromScript\nShouldBeVisibleFromScript().x".toScriptSource()

    val funAndValAccessScriptText = "$thisPackage.funShouldBeVisibleFromScript($thisPackage.valShouldBeVisibleFromScript)"
    private val funAndValAccessScript = funAndValAccessScriptText.toScriptSource()

    private val funAndValImportScriptText =
        """
            import $thisPackage.funShouldBeVisibleFromScript
            import $thisPackage.valShouldBeVisibleFromScript
            funShouldBeVisibleFromScript(valShouldBeVisibleFromScript)
        """.trimMargin()
    private val funAndValImportScript = funAndValImportScriptText.toScriptSource()

    @Test
    fun testResolveClassFromClassloader() {
        runScriptAndCheckResult(classAccessScript, configurationWithDependenciesFromClassloader, null, 42)
        runScriptAndCheckResult(classImportScript, configurationWithDependenciesFromClassloader, null, 42)
    }

    @Test
    fun testResolveClassFromClasspath() {
        runScriptAndCheckResult(classAccessScript, configurationWithDependenciesFromClasspath, null, 42)
        runScriptAndCheckResult(classImportScript, configurationWithDependenciesFromClasspath, null, 42)
    }

    @Test
    fun testResolveFunAndValFromClassloader() {
        runScriptAndCheckResult(funAndValAccessScript, configurationWithDependenciesFromClassloader, null, 42)
        runScriptAndCheckResult(funAndValImportScript, configurationWithDependenciesFromClassloader, null, 42)
    }

    @Test
    fun testReplResolveFunAndValFromClassloader() {
        checkEvaluateInRepl(
            sequenceOf(funAndValAccessScriptText, funAndValAccessScriptText), sequenceOf(42, 42),
            configurationWithDependenciesFromClassloader,
            null
        )
        checkEvaluateInRepl(
            funAndValImportScriptText.split('\n').asSequence(), sequenceOf(null, null, 42),
            configurationWithDependenciesFromClassloader,
            null
        )
        runScriptAndCheckResult(funAndValImportScript, configurationWithDependenciesFromClassloader, null, 42)
    }

    @Test
    fun testResolveFunAndValFromClasspath() {
        runScriptAndCheckResult(funAndValAccessScript, configurationWithDependenciesFromClasspath, null, 42)
        runScriptAndCheckResult(funAndValImportScript, configurationWithDependenciesFromClasspath, null, 42)
    }

    @Test
    fun testResolveClassFromClassloaderIsolated() {
        val evaluationConfiguration = ScriptEvaluationConfiguration {
            jvm {
                baseClassLoader(null)
            }
        }
        runScriptAndCheckResult(classAccessScript, configurationWithDependenciesFromClassloader, evaluationConfiguration, 42)
    }

    @Test
    fun testResolveClassesFromClassloaderAndClassPath() {
        val script = """
            org.jetbrains.kotlin.mainKts.MainKtsConfigurator()
            ${thisPackage}.ShouldBeVisibleFromScript().x
        """.trimIndent().toScriptSource()
        val classpath = listOf(
            File("dist/kotlinc/lib/kotlin-main-kts.jar").also {
                assertTrue("kotlin-main-kts.jar not found, run dist task: ${it.absolutePath}", it.exists())
            }
        )
        val compilationConfiguration = configurationWithDependenciesFromClassloader.with {
            updateClasspath(classpath)
        }
        runScriptAndCheckResult(script, compilationConfiguration, null, 42)
    }

    private fun <T> runScriptAndCheckResult(
        script: SourceCode,
        compilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration?,
        expectedResult: T
    ) {
        val res = BasicJvmScriptingHost().eval(script, compilationConfiguration, evaluationConfiguration).valueOrThrow().returnValue
        when (res) {
            is ResultValue.Value -> assertEquals(expectedResult, res.value)
            is ResultValue.Error -> throw res.error
            else -> throw Exception("Unexpected evaluation result: $res")
        }
    }
}

@Suppress("unused")
class ShouldBeVisibleFromScript {
    val x = 42
}

@Suppress("unused")
fun funShouldBeVisibleFromScript(x: Int) = x * 7

@Suppress("unused")
val valShouldBeVisibleFromScript = 6
