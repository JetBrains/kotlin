/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts.test

import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR
import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.*
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class MainKtsIT {

    @Test
    fun testResolveJunit() {
        runWithKotlincAndMainKts("$TEST_DATA_ROOT/hello-resolve-junit.main.kts", listOf("Hello, World!"))
    }

    @Test
    @Ignore // Fails on TC most likely due to repo proxying
    fun testKotlinxHtml() {
        runWithK2JVMCompilerAndMainKts(
            "$TEST_DATA_ROOT/kotlinx-html.main.kts",
            listOf("<html>", "  <body>", "    <h1>Hello, World!</h1>", "  </body>", "</html>")
        )
    }

    @Test
    fun testImport() {
        val mainKtsJar = File("dist/kotlinc/lib/kotlin-main-kts.jar")
        Assert.assertTrue("kotlin-main-kts.jar not found, run dist task: ${mainKtsJar.absolutePath}", mainKtsJar.exists())

        runWithK2JVMCompiler(
            "$TEST_DATA_ROOT/import-test.main.kts",
            listOf("Hi from common", "Hi from middle", "Hi from main", "sharedVar == 5"),
            classpath = listOf(mainKtsJar)
        )
    }

    @Test
    fun testThreadContextClassLoader() {
        runWithKotlincAndMainKts("$TEST_DATA_ROOT/context-classloader.main.kts", listOf("MainKtsConfigurator"))
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun testCachedReflection() {
        val cache = createTempDirectory("main.kts.test")

        try {
            runWithKotlinRunner("$TEST_DATA_ROOT/use-reflect.main.kts", listOf("false"), cacheDir = cache)
            // second run uses the cached script
            runWithKotlinRunner("$TEST_DATA_ROOT/use-reflect.main.kts", listOf("false"), cacheDir = cache)
        } finally {
            cache.toFile().deleteRecursively()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun testCache() {
        val script = File("$TEST_DATA_ROOT/import-test.main.kts").absolutePath
        val cache = createTempDirectory("main.kts.test")

        try {
            Assert.assertTrue(cache.exists() && cache.listDirectoryEntries("*.jar").isEmpty())
            runWithKotlinRunner(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
            val cacheFile = cache.listDirectoryEntries("*.jar").firstOrNull()
            Assert.assertTrue(cacheFile != null && cacheFile.exists())

            // run generated jar with java
            val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
            val args = listOf(javaExecutable.absolutePath, "-jar", cacheFile!!.toString())
            runAndCheckResults(
                args, OUT_FROM_IMPORT_TEST,
                additionalEnvVars = listOf(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to cache.toAbsolutePath().toString())
            )

            // this run should use the cached script
            runWithKotlinRunner(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
        } finally {
            cache.toFile().deleteRecursively()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun testCacheInProcess() {
        val script = File("$TEST_DATA_ROOT/import-test.main.kts").absolutePath
        val cache = createTempDirectory("main.kts.test")

        try {
            Assert.assertTrue(cache.exists() && cache.listDirectoryEntries("*.jar").isEmpty())
            runWithK2JVMCompilerAndMainKts(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
            val cacheFile = cache.listDirectoryEntries("*.jar").firstOrNull()
            Assert.assertTrue(cacheFile != null && cacheFile.exists())

            // run generated jar with java
            val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
            val args = listOf(javaExecutable.absolutePath, "-jar", cacheFile!!.toString())
            runAndCheckResults(
                args, OUT_FROM_IMPORT_TEST,
                additionalEnvVars = listOf(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to cache.toAbsolutePath().toString())
            )

            // this run should use the cached script
            runWithK2JVMCompilerAndMainKts(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
        } finally {
            cache.toFile().deleteRecursively()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun testCacheWithFileLocation() {
        val scriptPath = File("$TEST_DATA_ROOT/script-file-location-default.main.kts").absolutePath
        val cache = createTempDirectory("main.kts.test")
        val expectedTestOutput = listOf(Regex.escape(scriptPath))

        try {
            Assert.assertTrue(cache.exists() && cache.listDirectoryEntries("*.jar").isEmpty())
            runWithKotlinRunner(scriptPath, expectedTestOutput, cacheDir = cache)
            val cacheFile = cache.listDirectoryEntries("*.jar").firstOrNull()
            Assert.assertTrue(cacheFile != null && cacheFile.exists())

            // this run should use the cached script
            runWithKotlinRunner(scriptPath, expectedTestOutput, cacheDir = cache)
        } finally {
            cache.toFile().deleteRecursively()
        }
    }

    @Test
    fun testHelloSerialization() {
        val paths = PathUtil.kotlinPathsForDistDirectory
        val serializationPlugin = paths.jar(KotlinPaths.Jar.SerializationPlugin)
        runWithKotlinc(
            arrayOf(
                "-Xplugin=${serializationPlugin.absolutePath}",
                "-cp", paths.jar(KotlinPaths.Jar.MainKts).absolutePath,
                "-script", File("$TEST_DATA_ROOT/hello-kotlinx-serialization.main.kts").absolutePath
            ),
            listOf("""\{"firstName":"James","lastName":"Bond"\}""", "User\\(firstName=James, lastName=Bond\\)")
        )
    }

    @Test
    fun testUtf8Bom() {
        val scriptPath = "$TEST_DATA_ROOT/utf8bom.main.kts"
        Assert.assertTrue("Expect file '$scriptPath' to start with UTF-8 BOM", File(scriptPath).readText().startsWith(UTF8_BOM))
        runWithKotlincAndMainKts(scriptPath, listOf("Hello world"))
    }
}

fun runWithKotlincAndMainKts(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    cacheDir: Path? = null
) {
    val paths = PathUtil.kotlinPathsForDistDirectory
    runWithKotlinc(
        scriptPath, expectedOutPatterns, expectedExitCode,
        classpath = listOf(
            paths.jar(KotlinPaths.Jar.MainKts).also {
                Assert.assertTrue("kotlin-main-kts.jar not found, run dist task: ${it.absolutePath}", it.exists())
            }
        ),
        additionalEnvVars = listOf(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to (cacheDir?.toAbsolutePath()?.toString() ?: ""))
    )
}

fun runWithKotlinRunner(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    cacheDir: Path? = null,
    expectErrorOnK2: Boolean = false
) {
    runWithKotlinLauncherScript(
        "kotlin", listOf(scriptPath), expectedOutPatterns, expectedExitCode,
        additionalEnvVars = listOf(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to (cacheDir?.toAbsolutePath()?.toString() ?: "")),
        expectErrorOnK2 = expectErrorOnK2
    )
}

fun runWithK2JVMCompilerAndMainKts(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    cacheDir: Path? = null
) {
    withProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, cacheDir?.toAbsolutePath()?.toString() ?: "") {
        runWithK2JVMCompiler(
            scriptPath, expectedOutPatterns, expectedExitCode,
            classpath = listOf(
                File("dist/kotlinc/lib/kotlin-main-kts.jar").also {
                    Assert.assertTrue("kotlin-main-kts.jar not found, run dist task: ${it.absolutePath}", it.exists())
                }
            )
        )
    }
}

internal const val UTF8_BOM = 0xfeff.toChar().toString()

