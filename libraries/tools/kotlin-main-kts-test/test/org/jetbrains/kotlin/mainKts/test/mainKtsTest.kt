/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.mainKts.test

import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_PROPERTY
import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.junit.Assert
import org.junit.Test
import java.io.*
import java.net.URLClassLoader
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

fun evalFile(scriptFile: File, cacheDir: File? = null): ResultWithDiagnostics<EvaluationResult> =
    withProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, cacheDir?.absolutePath ?: "") {
        val scriptDefinition = createJvmCompilationConfigurationFromTemplate<MainKtsScript>()

        val evaluationEnv = ScriptEvaluationConfiguration {
            jvm {
                baseClassLoader(null)
            }
            constructorArgs(emptyArray<String>())
            enableScriptsInstancesSharing()
        }

        BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), scriptDefinition, evaluationEnv)
    }


const val TEST_DATA_ROOT = "libraries/tools/kotlin-main-kts-test/testData"

class MainKtsTest {

    @Test
    fun testResolveJunit() {
        val res = evalFile(File("$TEST_DATA_ROOT/hello-resolve-junit.main.kts"))
        assertSucceeded(res)
    }

//    @Test
    // this test is disabled: the resolving works fine, but ivy resolver is not processing "pom"-type dependencies correctly (
    //  as far as I can tell)
    // TODO: 1. find non-default but non-pom dependency suitable for an example to test resolving
    // TODO: 2. implement proper handling of pom-typed dependencies (e.g. consider to reimplement it on aether as in JarRepositoryManager (from IDEA))
    fun testResolveWithArtifactType() {
        val res = evalFile(File("$TEST_DATA_ROOT/resolve-moneta.main.kts"))
        assertSucceeded(res)
    }

    @Test
    fun testResolveJunitDynamicVer() {
        val errRes = evalFile(File("$TEST_DATA_ROOT/hello-resolve-junit-dynver-error.main.kts"))
        assertFailed("Unresolved reference: assertThrows", errRes)

        val res = evalFile(File("$TEST_DATA_ROOT/hello-resolve-junit-dynver.main.kts"))
        assertSucceeded(res)
    }

    @Test
    fun testUnresolvedJunit() {
        val res = evalFile(File("$TEST_DATA_ROOT/hello-unresolved-junit.main.kts"))
        assertFailed("Unresolved reference: junit", res)
    }

    @Test
    fun testResolveError() {
        val res = evalFile(File("$TEST_DATA_ROOT/hello-resolve-error.main.kts"))
        assertFailed("File 'abracadabra' not found", res)
    }

    @Test
    fun testResolveLog4jAndDocopt() {
        val res = evalFile(File("$TEST_DATA_ROOT/resolve-log4j-and-docopt.main.kts"))
        assertSucceeded(res)
    }

    private val outFromImportTest = listOf("Hi from common", "Hi from middle", "sharedVar == 5")

    @Test
    fun testImport() {

        val out = captureOut {
            val res = evalFile(File("$TEST_DATA_ROOT/import-test.main.kts"))
            assertSucceeded(res)
        }.lines()

        Assert.assertEquals(outFromImportTest, out)
    }

    @Test
    fun testCompilerOptions() {

        val out = captureOut {
            val res = evalFile(File("$TEST_DATA_ROOT/compile-java6.main.kts"))
            assertSucceeded(res)
            assertIsJava6Bytecode(res)
        }.lines()

        Assert.assertEquals(listOf("Hi from sub", "Hi from super", "Hi from random"), out)
    }

    @Test
    fun testCache() {
        val script = File("$TEST_DATA_ROOT/import-test.main.kts")
        val cache = createTempDir("main.kts.test")

        try {
            Assert.assertTrue(cache.exists() && cache.listFiles { f: File -> f.extension == "jar" }?.isEmpty() == true)
            val out1 = evalSuccessWithOut(script)
            Assert.assertEquals(outFromImportTest, out1)
            Assert.assertTrue(cache.listFiles { f: File -> f.extension.equals("jar", ignoreCase = true) }?.isEmpty() == true)

            val out2 = evalSuccessWithOut(script, cache)
            Assert.assertEquals(outFromImportTest, out2)
            val casheFile = cache.listFiles { f: File -> f.extension.equals("jar", ignoreCase = true) }?.firstOrNull()
            Assert.assertTrue(casheFile != null && casheFile.exists())

            val out3 = captureOut {
                val classLoader = URLClassLoader(arrayOf(casheFile!!.toURI().toURL()), null)
                val clazz = classLoader.loadClass("Import_test_main")
                val mainFn = clazz.getDeclaredMethod("main", Array<String>::class.java)
                mainFn.invoke(null, arrayOf<String>())
            }.lines()
            Assert.assertEquals(outFromImportTest, out3)
        } finally {
            cache.deleteRecursively()
        }
    }

    private fun assertIsJava6Bytecode(res: ResultWithDiagnostics<EvaluationResult>) {
        val scriptClassResource = res.valueOrThrow().returnValue.scriptClass!!.java.run {
            getResource("$simpleName.class")
        }

        DataInputStream(ByteArrayInputStream(scriptClassResource.readBytes())).use { stream ->
            val header = stream.readInt()
            if (0xCAFEBABE.toInt() != header) throw IOException("Invalid header class header: $header")
            val minor = stream.readUnsignedShort()
            val major = stream.readUnsignedShort()
            Assert.assertTrue(major == 50)
        }
    }

    private fun assertSucceeded(res: ResultWithDiagnostics<EvaluationResult>) {
        Assert.assertTrue(
            "test failed:\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Success
        )
    }

    private fun assertFailed(expectedError: String, res: ResultWithDiagnostics<EvaluationResult>) {
        Assert.assertTrue(
            "test failed - expecting a failure with the message \"$expectedError\" but received " +
                    (if (res is ResultWithDiagnostics.Failure) "failure" else "success") +
                    ":\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Failure && res.reports.any { it.message.contains("$expectedError") }
        )
    }

    private fun evalSuccessWithOut(scriptFile: File, cacheDir: File? = null): List<String> =
        captureOut {
            val res = evalFile(scriptFile, cacheDir)
            assertSucceeded(res)
        }.lines()
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
    return outStream.toString().trim()
}

internal fun <T> withProperty(name: String, value: String?, body: () -> T): T {
    val prevCacheDir = System.getProperty(name)
    if (value == null) System.clearProperty(name)
    else System.setProperty(name, value)
    try {
        return body()
    } finally {
        if (prevCacheDir == null) System.clearProperty(name)
        else System.setProperty(name, prevCacheDir)
    }
}