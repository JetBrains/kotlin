/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223.daemon.test

import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.jsr223.daemon.KotlinJsr223DaemonScriptEngineFactory
import org.jetbrains.kotlin.jsr223.daemon.KotlinJsr223DaemonScriptEngineImpl
import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.script.ScriptException
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Ports a few jsr223-specific main-kts tests (see `MainKtsJsr223Test` in `kotlin-main-kts-test`) to
 * test [KotlinJsr223DaemonScriptEngineFactory]'s custom-script-definition support with a
 * non-trivial script definition.
 */
class KotlinJsr223DaemonScriptEngineMainKtsTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val compilerClasspath: List<File> = classpathFromSystemProperty("kotlinJsr223DaemonCompilerClasspath")

    private val mainKtsScriptDefinition = createJvmScriptDefinitionFromTemplate<MainKtsScript>()

    private val enginesToShutDown = mutableListOf<KotlinJsr223DaemonScriptEngineImpl>()

    private val dirsToDeleteAfterEngines = mutableListOf<Path>()

    private fun newEngine(
        withMainKtsOnCompileClasspath: Boolean = false,
        withSerializationRuntime: Boolean = false,
    ): KotlinJsr223DaemonScriptEngineImpl {
        val additionalClasspath = buildList {
            addAll(listOf(stdlibPath, scriptRuntimePath))
            if (withSerializationRuntime) addAll(serializationRuntimeClasspath())
            if (withMainKtsOnCompileClasspath) addAll(mainKtsPaths)
        }
        val factory = KotlinJsr223DaemonScriptEngineFactory(
            compilerClasspath = compilerClasspath,
            additionalClasspath = additionalClasspath,
            daemonOptions = DaemonOptions(
                runFilesPath = daemonRunDir.resolve("run").toString(),
                shutdownDelayMilliseconds = 0,
            ),
            daemonLogOptions = DaemonLogOptions(logsPath = daemonRunDir.resolve("logs").toString()),
            baseCompilationConfiguration = mainKtsScriptDefinition.compilationConfiguration,
            baseEvaluationConfiguration = mainKtsScriptDefinition.evaluationConfiguration,
        )
        return (factory.scriptEngine as KotlinJsr223DaemonScriptEngineImpl).also { enginesToShutDown += it }
    }

    @AfterEach
    fun tearDown() {
        for (engine in enginesToShutDown) {
            engine.forceShutdownDaemonForTests()
        }
        enginesToShutDown.clear()
        for (dir in dirsToDeleteAfterEngines) {
            // Best-effort: on Windows, jars loaded by the (never closed) evaluation class loader may stay locked.
            dir.toFile().deleteRecursively()
        }
        dirsToDeleteAfterEngines.clear()
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
    fun testWithDependsOn() {
        // Local repository to avoid network access.
        // Not a `@TempDir`: the evaluation class loader created for the resolved dependency is never closed, so on Windows
        // the jar stays locked after the engine is shut down, and the strict `@TempDir` cleanup fails the test.
        val repositoryDir = Files.createTempDirectory("jsr223-daemon-repository-")
        dirsToDeleteAfterEngines.add(repositoryDir)
        val artifact = singleClassJar(DependsOnProbe::class.java, repositoryDir.resolve("probe.jar").toFile())
        val engine = newEngine(withMainKtsOnCompileClasspath = true)

        // `DependsOnProbe` is not on the snippet compile classpath.
        assertEquals(
            DependsOnProbe.VALUE,
            engine.eval(
                """
                    @file:Repository("${repositoryDir.toAbsolutePath().invariantSeparatorsPathString}")
                    @file:DependsOn("${artifact.name}")
                    ${DependsOnProbe::class.java.name}.VALUE
                """.trimIndent()
            )
        )

        assertEquals("${DependsOnProbe.VALUE}!", engine.eval("${DependsOnProbe::class.java.name}.VALUE + \"!\""))
    }

    @Test
    fun testSerializationCompilerPluginPersistsAcrossSnippets() {
        val engine = newEngine(withMainKtsOnCompileClasspath = true, withSerializationRuntime = true)
        assertEquals(
            null,
            engine.eval(
                """
                    ${serializationPluginAnnotation()}
                    import kotlinx.serialization.Serializable

                    @Serializable
                    data class User(val name: String)
                """.trimIndent()
            )
        )
        assertEquals(
            null,
            engine.eval(
                """
                    ${serializationPluginAnnotation()}
                    import kotlinx.serialization.Serializable

                    @Serializable
                    data class Project(val name: String)
                """.trimIndent()
            )
        )

        val descriptorNames = engine.eval(
            "User.serializer().descriptor.serialName + \"|\" + Project.serializer().descriptor.serialName"
        ).toString().split('|')
        assertEquals(2, descriptorNames.size)
        assertTrue(descriptorNames[0].endsWith(".User"))
        assertTrue(descriptorNames[1].endsWith(".Project"))
    }

    @Test
    fun testSeveralCompilerPluginsRequestedBySingleSnippetPersistAcrossSnippets() {
        val engine = newEngine(withMainKtsOnCompileClasspath = true, withSerializationRuntime = true)
        assertEquals(
            null,
            engine.eval(
                """
                    ${serializationPluginAnnotation()}
                    ${allOpenPluginAnnotation()}
                    import kotlinx.serialization.Serializable

                    @Serializable
                    class User(val name: String)
                """.trimIndent()
            )
        )
        // The all-open plugin makes serializable classes open, so they can be subclassed.
        assertEquals(
            null,
            engine.eval(
                """
                    import kotlinx.serialization.Serializable

                    class Admin(name: String) : User(name)

                    @Serializable
                    class Project(val name: String)

                    class InternalProject(name: String) : Project(name)
                """.trimIndent()
            )
        )

        val descriptorNames = engine.eval(
            "User.serializer().descriptor.serialName + \"|\" + Project.serializer().descriptor.serialName"
        ).toString().split('|')
        assertEquals(2, descriptorNames.size)
        assertTrue(descriptorNames[0].endsWith(".User"))
        assertTrue(descriptorNames[1].endsWith(".Project"))
        assertEquals("a|b", engine.eval("Admin(\"a\").name + \"|\" + InternalProject(\"b\").name"))
    }

    @Test
    fun testSerializationCompilerPluginConflictKeepsOriginalOptions() {
        val engine = newEngine(withMainKtsOnCompileClasspath = true, withSerializationRuntime = true)
        engine.eval(
            """
                ${serializationPluginAnnotation()}
                import kotlinx.serialization.Serializable

                @Serializable
                data class User(val name: String)
            """.trimIndent()
        )

        val conflict = assertFailsWith<ScriptException> {
            engine.eval(
                """
                    ${serializationPluginAnnotation("disableIntrinsic=true")}
                    import kotlinx.serialization.Serializable

                    @Serializable
                    data class Project(val name: String)
                """.trimIndent()
            )
        }
        assertContains(conflict.message.orEmpty(), "conflicting options for compiler plugin", ignoreCase = true)
        assertTrue(engine.eval("User.serializer().descriptor.serialName").toString().endsWith(".User"))
    }

    @Test
    fun testSerializationCompilerPluginIsNotCommittedAfterFailedBatch() {
        val engine = newEngine(withMainKtsOnCompileClasspath = true, withSerializationRuntime = true)
        assertFailsWith<ScriptException> {
            engine.eval(
                """
                    ${serializationPluginAnnotation()}
                    import kotlinx.serialization.Serializable

                    @Serializable
                    data class Broken(val name: String)
                    unresolvedName
                """.trimIndent()
            )
        }

        val afterRollback = assertFailsWith<ScriptException> {
            engine.eval(
                """
                    import kotlinx.serialization.Serializable

                    @Serializable
                    data class AfterFailure(val name: String)
                    AfterFailure.serializer().descriptor.serialName
                """.trimIndent()
            )
        }
        assertContains(afterRollback.message.orEmpty(), "serializer", ignoreCase = true)
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
private val TEST_DATA_ROOT = File("libraries/tools/kotlin-main-kts-test/testData").absoluteFile.invariantSeparatorsPath

private fun serializationPluginAnnotation(options: String? = null): String {
    val optionArgument = options?.let { ", \"$it\"" }.orEmpty()
    return """@file:CompilerPlugin("${serializationPluginClasspathForScript()}"$optionArgument)"""
}

private fun allOpenPluginAnnotation(): String {
    val classpath = System.getProperty("kotlin.script.test.allopen.plugin.classpath")!!
        .split(File.pathSeparator)
        .joinToString(File.pathSeparator) { it.replace(File.separatorChar, '/') }
    return """@file:CompilerPlugin("$classpath", "annotation=kotlinx.serialization.Serializable")"""
}

private fun serializationPluginClasspathForScript(): String =
    System.getProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath")!!
        .split(File.pathSeparator)
        .joinToString(File.pathSeparator) { it.replace(File.separatorChar, '/') }

@Suppress("DEPRECATION_ERROR")
private fun serializationRuntimeClasspath(): List<Path> =
    internalScriptingRunSuspend {
        CompoundDependenciesResolver(MavenDependenciesResolver())
            .resolve("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
            .valueOrThrow()
            .map(File::toPath)
    }

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
