/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.mainKts.test

import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_PROPERTY
import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.jetbrains.kotlin.mainKts.SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME
import org.jetbrains.kotlin.mainKts.impl.Directories
import org.jetbrains.kotlin.scripting.compiler.plugin.assertTrue
import org.jetbrains.kotlin.scripting.compiler.plugin.expectTestToFailOnK2
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate

fun evalFile(scriptFile: File, cacheDir: File? = null): ResultWithDiagnostics<EvaluationResult> =
    withProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, cacheDir?.absolutePath ?: "") {
        evalFileWithConfigurations(scriptFile)
    }

fun evalFileWithConfigurations(
    scriptFile: File,
    compilation: ScriptCompilationConfiguration.Builder.() -> Unit = {},
    evaluation: ScriptEvaluationConfiguration.Builder.() -> Unit = {}
): ResultWithDiagnostics<EvaluationResult> {
    val scriptDefinition = createJvmScriptDefinitionFromTemplate<MainKtsScript>(
        compilation = compilation,
        evaluation = {
            evaluation()
            jvm {
                baseClassLoader(null)
            }
            constructorArgs(emptyArray<String>())
            enableScriptsInstancesSharing()
        }
    )

    return BasicJvmScriptingHost().eval(
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
                    resErr.reports.any { it.message.contains("Unresolved reference") && it.message.contains("hamcrest") }
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
        assertFailed("Unresolved reference 'junit'.", res)
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
    fun testImportWithCapture() {

        val out = captureOut {
            val res = evalFile(File("$TEST_DATA_ROOT/import-with-capture-test.main.kts"))
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
        // TODO: the second error is due to the late cycle detection, see TODO in makeCompiledScript$makeOtherScripts
        // TODO: third error is due to the early IR backend error, consider processing it in makeCompiledScript$makeOtherScripts
        assertFailedAny("Unable to handle recursive script dependencies", "is already bound", "Duplicate JVM class name", res = res)
    }

    @Test
    fun testCompilerOptions() {
        val out = captureOut {
            val res = evalFile(File("$TEST_DATA_ROOT/compiler-options.main.kts"))
            assertSucceeded(res)

            // `-Xabi-stability=unstable` unsets 5th bit in `Metadata.extraInt` (see kdoc). Let's check that it had effect.
            val scriptClass = res.valueOrThrow().returnValue.scriptClass!!.java
            val metadata = scriptClass.declaredAnnotations.single { it.annotationClass.java.name == "kotlin.Metadata" }
            val extraInt = metadata.javaClass.getDeclaredMethod("xi").invoke(metadata) as Int
            assertTrue(extraInt and (1 shl 5) == 0) {
                "Incorrect value of Metadata.extraInt; probably the compiler flag -Xabi-stability=unstable wasn't recognized: $extraInt"
            }
        }.lines()

        assertEquals(listOf("Hi from sub", "Hi from super", "Hi from random"), out)
    }

    @Test
    fun testScriptFileLocationDefaultVariable() {
        val resOk = evalFile(File("$TEST_DATA_ROOT/script-file-location-default.main.kts"))
        assertSucceeded(resOk)
        val resultValue = resOk.valueOrThrow().returnValue
        assertTrue(resultValue is ResultValue.Value) { "Result value should be of type Value" }
        val value = (resultValue as ResultValue.Value).value!!
        assertEquals("String", value::class.simpleName)
        val expectedPathSuffix = "libraries/tools/kotlin-main-kts-test/testData/script-file-location-default.main.kts"
        val actualPath = (value as String).replace("\\", "/")
        assertTrue(actualPath.endsWith(expectedPathSuffix)) { "Script file path does not end with expected path" }
    }

    @Test
    fun testScriptFileLocationCustomizedVariable() {
        val resOk = evalFile(File("$TEST_DATA_ROOT/script-file-location-customized.main.kts"))
        assertSucceeded(resOk)
        val resultValue = resOk.valueOrThrow().returnValue
        assertTrue(resultValue is ResultValue.Value) { "Result value should be of type Value" }
        val value = (resultValue as ResultValue.Value).value!!
        assertEquals("String", value::class.simpleName)
        val expectedPathSuffix = "libraries/tools/kotlin-main-kts-test/testData/script-file-location-customized.main.kts"
        val actualPath = (value as String).replace("\\", "/")
        assertTrue(actualPath.endsWith(expectedPathSuffix)) { "Script file path does not end with expected path" }
    }

    @Test
    fun testScriptFileLocationWithImportedScript() {
        val resOk = evalFile(File("$TEST_DATA_ROOT/script-file-location-with-imported-file.main.kts"))
        assertSucceeded(resOk)
        val resultValue = resOk.valueOrThrow().returnValue
        assertTrue(resultValue is ResultValue.Value) { "Result value should be of type Value" }
        val value = (resultValue as ResultValue.Value).value!!
        assertEquals("Array", value::class.simpleName)
        val expectedSelfPathSuffix = "libraries/tools/kotlin-main-kts-test/testData/script-file-location-with-imported-file.main.kts"
        val expectedImportedPathSuffix = "libraries/tools/kotlin-main-kts-test/testData/script-file-location-helper-imported-file.main.kts"
        val actualPathSelf = (value as Array<*>)[0].toString().replace("\\", "/")
        val actualPathImported = value[1].toString().replace("\\", "/")
        assertTrue(actualPathSelf.endsWith(expectedSelfPathSuffix)) { "Script file path does not end with expected path" }
        assertTrue(actualPathImported.endsWith(expectedImportedPathSuffix)) { "Script file path does not end with expected path" }
    }

    @Test
    fun testScriptFileLocationDefaultVariableNotAvailableIfScriptFileVariableCustomized() {
        val resFailed = evalFile(File("$TEST_DATA_ROOT/script-file-location-customized-default-not-available.main.kts"))
        assertFailed("Unresolved reference: $SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME", resFailed)
    }

    @Test
    @Ignore // Overriding provided properties is no supported yet, the test was working by errorneous coincidence. See #KT-52986
    fun ignore_testScriptFileLocationDefaultVariableRedefinition() {
        val resOk = evalFile(File("$TEST_DATA_ROOT/script-file-location-redefine-variable.kts"))
        assertSucceeded(resOk)
        val resultValue = resOk.valueOrThrow().returnValue
        assertTrue(resultValue is ResultValue.Value) { "Result value should be of type Value" }
        val value = (resultValue as ResultValue.Value).value!!
        assertEquals("String", value::class.simpleName)
        assertEquals("success", value)
    }

    @Test
    fun testKt48812() {
        val res = evalFile(File("$TEST_DATA_ROOT/kt48812.main.kts"))
        assertSucceeded(res)
    }

    @Test
    fun testHelloSerialization() = expectTestToFailOnK2 {
        // the embeddable plugin is needed for this test, because embeddable compiler is used.
        val serializationPluginClasspath = System.getProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath")!!
        val out = captureOut {
            val res = evalFileWithConfigurations(
                File("$TEST_DATA_ROOT/hello-kotlinx-serialization.main.kts"),
                compilation = {
                    compilerOptions(
                        "-Xplugin=$serializationPluginClasspath"
                    )
                }
            )
            assertSucceeded(res)
        }.lines()
        assertEquals(
            listOf("""{"firstName":"James","lastName":"Bond"}""", "User(firstName=James, lastName=Bond)"),
            out
        )
    }

    @Test
    fun testUtf8Bom() {
        val scriptPath = "$TEST_DATA_ROOT/utf8bom.main.kts"
        Assert.assertTrue("Expect file '$scriptPath' to start with UTF-8 BOM", File(scriptPath).readText().startsWith(UTF8_BOM))
        val res = evalFile(File(scriptPath))
        assertSucceeded(res)
    }

    @Test
    fun testUseSlf4j() {
        val err = captureOutAndErr {
            val res = evalFile(File("$TEST_DATA_ROOT/use-slf4j.main.kts"))
            assertSucceeded(res)
        }.second
        Assert.assertTrue(
            "Expect info log line with \"test-slf4j\" text, got:\n$err",
            err.contains("INFO  - test-slf4j")
        )
    }

    private fun assertSucceeded(res: ResultWithDiagnostics<EvaluationResult>) {
        Assert.assertTrue(
            "test failed:\n  ${res.reports.joinToString("\n  ") { it.severity.name + ": " + it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Success
        )
    }

    private fun assertFailed(expectedError: String, res: ResultWithDiagnostics<EvaluationResult>) {
        assertFailedAny(expectedError, res = res)
    }

    private fun assertFailedAny(vararg expectedErrors: String, res: ResultWithDiagnostics<EvaluationResult>) {
        val reports = res.reports.map { diag ->
            diag.message +
                    generateSequence(diag.exception) { it.cause }
                        .filter { !(it.message != null && diag.message.contains(it.message!!)) }
                        .joinToString("\n  Caused by: ", "\n  ") { it.message ?: it.toString() }
        }
        val expected = when (expectedErrors.size) {
            0 -> ""
            1 -> " with the message \"${expectedErrors[0]}\""
            else -> " with any of the messages: ${expectedErrors.joinToString("\", \"", "\"", "\";")}"
        }
        Assert.assertTrue(
            "test failed - expecting a failure$expected but received " +
                    (if (res is ResultWithDiagnostics.Failure) "failure" else "success") +
                    ":\n  ${reports.joinToString("\n  ")}",
            res is ResultWithDiagnostics.Failure && reports.any { report -> expectedErrors.any { report.containsIgnoringPunctuation(it) } }
        )
    }

    private val regexNonWord = "\\W".toRegex()
    private fun String.containsIgnoringPunctuation(it: String): Boolean {
        return this.replace(regexNonWord, "").contains(it.replace(regexNonWord, ""))
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

internal fun captureOut(body: () -> Unit): String = captureOutAndErr(body).first

internal fun captureOutAndErr(body: () -> Unit): Pair<String, String> {
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
    return outStream.toString().trim() to errStream.toString().trim()
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