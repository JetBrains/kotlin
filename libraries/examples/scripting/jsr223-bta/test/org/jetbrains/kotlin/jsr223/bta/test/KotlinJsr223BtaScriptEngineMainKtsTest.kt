/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223.bta.test

import org.jetbrains.kotlin.jsr223.bta.KotlinJsr223BtaScriptEngineFactory
import org.jetbrains.kotlin.jsr223.bta.KotlinJsr223BtaScriptEngineImpl
import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate

/**
 * Ports a few of `MainKtsJsr223Test`'s cases to exercise custom-script-definition support with a
 * non-trivial definition.
 */
class KotlinJsr223BtaScriptEngineMainKtsTest : BtaReplTestBase() {

    private val mainKtsScriptDefinition = createJvmScriptDefinitionFromTemplate<MainKtsScript>()

    private fun newEngine(withMainKtsOnCompileClasspath: Boolean = false): KotlinJsr223BtaScriptEngineImpl {
        val factory = KotlinJsr223BtaScriptEngineFactory(
            compilerClasspath = classpathFromSystemProperty("kotlinJsr223BtaImplClasspath"),
            scriptingPluginClasspath = classpathFromSystemProperty("kotlinJsr223BtaScriptingPluginClasspath"),
            additionalClasspath = listOf(stdlibPath, scriptRuntimePath) +
                    if (withMainKtsOnCompileClasspath) mainKtsPaths else emptyList(),
            daemonRunFilesPath = daemonRunFilesPath,
            daemonLogsPath = daemonLogsPath,
            daemonShutdownDelayMillis = 0,
            baseCompilationConfiguration = mainKtsScriptDefinition.compilationConfiguration,
            baseEvaluationConfiguration = mainKtsScriptDefinition.evaluationConfiguration,
        )
        return closeAfterTest(factory.scriptEngine as KotlinJsr223BtaScriptEngineImpl)
    }

    @Test
    fun testSimpleEval() {
        val engine = newEngine()
        val res1 = engine.eval("val x = 3")
        assertEquals(null, res1)
        val res2 = engine.eval("x + 2")
        assertEquals(5, res2)
    }

    @Test
    fun testWithDirectBindings() {
        val engine = newEngine()
        engine.put("z", 6)
        val res1 = engine.eval("val x = 7")
        assertEquals(null, res1)
        val res2 = engine.eval("z * x")
        assertEquals(42, res2)
    }

    @Test
    fun testDefinitionDefaultImportsAreVisibleInSnippets() {
        val engine = newEngine(withMainKtsOnCompileClasspath = true)
        assertEquals(null, engine.eval("val n = DependsOn::class.simpleName!!.length"))
        assertEquals("DependsOn9CompilerOptions", engine.eval("DependsOn::class.simpleName + n + CompilerOptions::class.simpleName"))
    }

    @Test
    fun testWithCompilerOptions() {
        val engine = newEngine(withMainKtsOnCompileClasspath = true)
        // Only works if the definition's `@CompilerOptions` handler runs inside the compiler.
        val res = engine.eval(
            """
                @file:CompilerOptions("-opt-in=kotlin.uuid.ExperimentalUuidApi")
                kotlin.uuid.Uuid.NIL.toString()
            """.trimIndent()
        )
        assertEquals("00000000-0000-0000-0000-000000000000", res)
    }

    @Test
    fun testWithDependsOn(@TempDir repositoryDir: Path) {
        // Local repository to avoid network access.
        val artifact = singleClassJar(DependsOnProbe::class.java, repositoryDir.resolve("probe.jar").toFile())
        val engine = newEngine(withMainKtsOnCompileClasspath = true)

        // `DependsOnProbe` is not on the snippet compile classpath.
        assertEquals(
            DependsOnProbe.VALUE,
            engine.eval(
                """
                    @file:Repository("${repositoryDir.toAbsolutePath()}")
                    @file:DependsOn("${artifact.name}")
                    ${DependsOnProbe::class.java.name}.VALUE
                """.trimIndent()
            )
        )

        assertEquals("${DependsOnProbe.VALUE}!", engine.eval("${DependsOnProbe::class.java.name}.VALUE + \"!\""))
    }

    @Test
    @Disabled("Same-batch @file:Import snippet chaining is not supported yet (codegen of cross-snippet accesses)")
    fun testWithImport() {
        val engine = newEngine()
        val res1 = engine.eval(
            """
                @file:Import("$TEST_DATA_ROOT/import-common.main.kts")
                @file:Import("$TEST_DATA_ROOT/import-middle.main.kts")
                sharedVar = sharedVar + 1
                sharedVar
            """.trimIndent()
        )
        assertEquals(5, res1)
    }
}

// Absolute, since `@file:Import` paths are resolved relative to the (temporary) snippet source file.
private val TEST_DATA_ROOT = File("libraries/tools/kotlin-main-kts-test/testData").absolutePath

object DependsOnProbe {
    const val VALUE: String = "resolved"
}

private fun singleClassJar(clazz: Class<*>, target: File): File {
    val entryName = clazz.name.replace('.', '/') + ".class"
    JarOutputStream(target.outputStream()).use { out ->
        out.putNextEntry(JarEntry(entryName))
        val classBytes = clazz.classLoader.getResourceAsStream(entryName)
            ?: error("cannot read the class file of ${clazz.name}")
        classBytes.use { it.copyTo(out) }
        out.closeEntry()
    }
    return target
}
