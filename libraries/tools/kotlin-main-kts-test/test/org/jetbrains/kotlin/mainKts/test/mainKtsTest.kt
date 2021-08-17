/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.mainKts.test

import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_PROPERTY
import org.jetbrains.kotlin.mainKts.impl.Directories
import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.jetbrains.kotlin.scripting.compiler.plugin.assertTrue
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.*
import java.util.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate

fun evalFile(scriptFile: File, cacheDir: File? = null): ResultWithDiagnostics<EvaluationResult> =
    withProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, cacheDir?.absolutePath ?: "") {
        val scriptDefinition = createJvmScriptDefinitionFromTemplate<MainKtsScript>(
            evaluation = {
                jvm {
                    baseClassLoader(null)
                }
                constructorArgs(emptyArray<String>())
                enableScriptsInstancesSharing()
            }
        )

        BasicJvmScriptingHost().eval(
            scriptFile.toScriptSource(), scriptDefinition.compilationConfiguration, scriptDefinition.evaluationConfiguration
        )
    }


const val TEST_DATA_ROOT = "libraries/tools/kotlin-main-kts-test/testData"
val OUT_FROM_IMPORT_TEST = listOf("Hi from common", "Hi from middle", "Hi from main", "sharedVar == 5")


class MainKtsTest {

    @Test
    fun testResolveJunit() {
        val res = evalFile(File("$TEST_DATA_ROOT/hello-resolve-junit.main.kts"))
        assertSucceeded(res)
    }

    @Test
    fun testResolveHamcrestViaJunit() {
        val resOk = evalFile(File("$TEST_DATA_ROOT/resolve-hamcrest-via-junit.main.kts"))
        assertSucceeded(resOk)

        val resErr = evalFile(File("$TEST_DATA_ROOT/resolve-error-hamcrest-via-junit.main.kts"))
        Assert.assertTrue(
            resErr is ResultWithDiagnostics.Failure &&
                    resErr.reports.any { it.message == "Unresolved reference: hamcrest" }
        )
    }

    @Test
    fun testResolveRuntimeDeps() {
        val resOk = evalFile(File("$TEST_DATA_ROOT/resolve-with-runtime.main.kts"))
        assertSucceeded(resOk)

        val resultValue = resOk.valueOrThrow().returnValue
        assertTrue(resultValue is ResultValue.Value) { "Result value should be of type Value" }
        val value = (resultValue as ResultValue.Value).value!!
        assertEquals("MimeTypedResult", value::class.simpleName)
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

    @Test
    fun testImport() {

        val out = captureOut {
            val res = evalFile(File("$TEST_DATA_ROOT/import-test.main.kts"))
            assertSucceeded(res)
        }.lines()

        Assert.assertEquals(OUT_FROM_IMPORT_TEST, out)
    }

    @Test
    fun testDuplicateImportError() {
        val res = evalFile(File("$TEST_DATA_ROOT/import-duplicate-test.main.kts"))
        assertFailed("Duplicate imports:", res)
    }

    @Test
    fun testCyclicImportError() {
        val res = evalFile(File("$TEST_DATA_ROOT/import-cycle-1.main.kts"))
        assertFailed("Unable to handle recursive script dependencies", res)
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

    private fun assertIsJava6Bytecode(res: ResultWithDiagnostics<EvaluationResult>) {
        val scriptClassResource = res.valueOrThrow().returnValue.scriptClass!!.java.run {
            getResource("$simpleName.class")
        }

        DataInputStream(ByteArrayInputStream(scriptClassResource.readBytes())).use { stream ->
            val header = stream.readInt()
            if (0xCAFEBABE.toInt() != header) throw IOException("Invalid header class header: $header")
            @Suppress("UNUSED_VARIABLE")
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

class CacheDirectoryDetectorTest {
    private val temp = "/test-temp-dir"
    private val home = "/test-home-dir"
    private val localAppData = "C:\\test-local-app-data"
    private val xdgCache = "/test-xdg-cache-dir"

    @Test
    fun `Windows uses local app data dir`() {
        setOSName("Windows 10")
        assertCacheDir(localAppData)
    }

    @Test
    fun `Windows falls back to temp dir when no app data dir`() {
        setOSName("Windows 10")
        environment.remove("LOCALAPPDATA")
        assertCacheDir(temp)
    }

    @Test
    fun `OS X uses user cache dir`() {
        setOSName("Mac OS X")
        assertCacheDir("$home/Library/Caches")
    }

    @Test
    fun `Linux uses XDG cache dir`() {
        setOSName("Linux")
        assertCacheDir(xdgCache)
    }

    @Test
    fun `Linux falls back to dot cache when no XDG dir`() {
        setOSName("Linux")
        environment.remove("XDG_CACHE_HOME")
        assertCacheDir("$home/.cache")
    }

    @Test
    fun `FreeBSD uses XDG cache dir`() {
        setOSName("FreeBSD")
        assertCacheDir(xdgCache)
    }

    @Test
    fun `FreeBSD falls back to dot cache when no XDG dir`() {
        setOSName("FreeBSD")
        environment.remove("XDG_CACHE_HOME")
        assertCacheDir("$home/.cache")
    }

    @Test
    fun `Unknown OS uses dot cache`() {
        setOSName("")
        assertCacheDir("$home/.cache")
    }

    @Test
    fun `Unknown OS and unknown home directory gives null`() {
        setOSName("")
        systemProperties.setProperty("user.home", "")
        assertCacheDir(null)
    }

    private fun setOSName(name: String?) {
        systemProperties.setProperty("os.name", name)
    }

    private fun assertCacheDir(path: String?) {
        val file = path?.let(::File)
        Assert.assertEquals(file, directories.cache)
    }

    private val systemProperties = Properties().apply {
        setProperty("java.io.tmpdir", temp)
        setProperty("user.home", home)
    }

    private val environment = mutableMapOf(
        "LOCALAPPDATA" to localAppData,
        "XDG_CACHE_HOME" to xdgCache
    )

    private val directories = Directories(systemProperties, environment)
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