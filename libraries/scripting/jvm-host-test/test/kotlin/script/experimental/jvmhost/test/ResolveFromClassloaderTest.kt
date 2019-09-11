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
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.JvmDependencyFromClassLoader
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ResolveFromClassloaderTest : TestCase() {

    @Test
    fun testResolveFromClassloader() {
        val script = "${ShouldBeVisibleFromScript::class.java.name}().x".toScriptSource()
        val compilationConfiguration = ScriptCompilationConfiguration {
            dependencies(JvmDependencyFromClassLoader { ShouldBeVisibleFromScript::class.java.classLoader })
        }
        val res = BasicJvmScriptingHost().eval(script, compilationConfiguration, null).valueOrThrow().returnValue as ResultValue.Value
        assertEquals(42, res.value)
    }

    @Test
    fun testResolveFromClassloaderIsolated() {
        val script = "${ShouldBeVisibleFromScript::class.java.name}().x".toScriptSource()
        val compilationConfiguration = ScriptCompilationConfiguration {
            dependencies(JvmDependencyFromClassLoader { ShouldBeVisibleFromScript::class.java.classLoader })
        }
        val evaluationConfiguration = ScriptEvaluationConfiguration {
            jvm {
                baseClassLoader(null)
            }
        }
        val res = BasicJvmScriptingHost().eval(script, compilationConfiguration, evaluationConfiguration).valueOrThrow().returnValue
        when (res) {
            is ResultValue.Value -> assertEquals(42, res.value)
            is ResultValue.Error -> throw res.error
            else -> throw Exception("Unexpected evaluation result: $res")
        }
    }

    @Test
    fun testResolveFromClassloaderAndClassPath() {
        val script = """
            org.jetbrains.kotlin.mainKts.MainKtsConfigurator()
            ${ShouldBeVisibleFromScript::class.java.name}().x
        """.trimIndent().toScriptSource()
        val classpath = listOf(
            File("dist/kotlinc/lib/kotlin-main-kts.jar").also {
                assertTrue("kotlin-main-kts.jar not found, run dist task: ${it.absolutePath}", it.exists())
            }
        )
        val compilationConfiguration = ScriptCompilationConfiguration {
            dependencies(
                JvmDependencyFromClassLoader { ShouldBeVisibleFromScript::class.java.classLoader },
                JvmDependency(classpath)
            )
        }
        val res = BasicJvmScriptingHost().eval(script, compilationConfiguration, null).valueOrThrow().returnValue as ResultValue.Value
        assertEquals(42, res.value)
    }
}

@Suppress("unused")
class ShouldBeVisibleFromScript {
    val x = 42
}
