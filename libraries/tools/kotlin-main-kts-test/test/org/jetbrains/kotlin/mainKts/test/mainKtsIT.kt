/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts.test

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR
import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.*
import org.jetbrains.kotlin.testFederation.SmokeTest
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

@SmokeTest
class MainKtsIT {

    @Test
    fun testResolveJunit() {
        runWithKotlincAndMainKts("$TEST_DATA_ROOT/hello-resolve-junit.main.kts", ["Hello, World!"])
    }

    @Test
    @Ignore // Fails on TC most likely due to repo proxying
    fun testKotlinxHtml() {
        runWithK2JVMCompilerAndMainKts(
            "$TEST_DATA_ROOT/kotlinx-html.main.kts",
            ["<html>", "  <body>", "    <h1>Hello, World!</h1>", "  </body>", "</html>"]
        )
    }

    @Test
    fun testImport() {
        val mainKtsJar = ForTestCompileRuntime.mainKtsJar()

        runWithK2JVMCompiler(
            "$TEST_DATA_ROOT/import-test.main.kts",
            ["Hi from common", "Hi from middle", "Hi from main", "sharedVar == 5"],
            classpath = [mainKtsJar]
        )
    }

    @Test
    fun testCompileWithImport() {
        val mainKtsJar = ForTestCompileRuntime.mainKtsJar()

        runWithK2JVMCompiler(
            "$TEST_DATA_ROOT/import-test.main.kts",
            classpath = [mainKtsJar],
            skipScriptArgument = true
        )
    }

    @Test
    fun testThreadContextClassLoader() {
        runWithKotlincAndMainKts("$TEST_DATA_ROOT/context-classloader.main.kts", ["MainKtsConfigurator"])
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun testCachedReflection() {
        val cache = createTempDirectory("main.kts.test")

        try {
            runWithKotlinRunner("$TEST_DATA_ROOT/use-reflect.main.kts", ["false"], cacheDir = cache)
            // second run uses the cached script
            runWithKotlinRunner("$TEST_DATA_ROOT/use-reflect.main.kts", ["false"], cacheDir = cache)
        } finally {
            cache.toFile().deleteRecursively()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun testCache() {
        if (isRunningTestOnK2) {
            // test fails on K1, which may cause some disruptions, but we plan to drop it very soon anyway, so maybe it's ok
            val script = File("$TEST_DATA_ROOT/import-test.main.kts").absolutePath
            val cache = createTempDirectory("main.kts.test")

            try {
                Assert.assertTrue(cache.exists() && cache.listDirectoryEntries("*.jar").isEmpty())
                runWithKotlinRunner(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
                val cacheFile = cache.listDirectoryEntries("*.jar").firstOrNull()
                Assert.assertTrue(cacheFile != null && cacheFile.exists())

                // run generated jar with java
                val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
                val args = [javaExecutable.absolutePath, "-jar", cacheFile!!.toString()]
                runAndCheckResults(
                    args, OUT_FROM_IMPORT_TEST,
                    additionalEnvVars = [COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to cache.toAbsolutePath().toString()]
                )

                // this run should use the cached script
                runWithKotlinRunner(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
            } finally {
                cache.toFile().deleteRecursively()
            }
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
            val args = [javaExecutable.absolutePath, "-jar", cacheFile!!.toString()]
            runAndCheckResults(
                args, OUT_FROM_IMPORT_TEST,
                additionalEnvVars = [COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to cache.toAbsolutePath().toString()]
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
        val expectedTestOutput = [Regex.escape(scriptPath)]

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
            [
                "-Xplugin=${serializationPlugin.absolutePath}",
                "-cp", paths.jar(KotlinPaths.Jar.MainKts).absolutePath,
                "-script", File("$TEST_DATA_ROOT/hello-kotlinx-serialization.main.kts").absolutePath
            ],
            ["""\{"firstName":"James","lastName":"Bond"\}""", "User\\(firstName=James, lastName=Bond\\)"]
        )
    }

    @Test
    fun testUtf8Bom() {
        val scriptPath = "$TEST_DATA_ROOT/utf8bom.main.kts"
        Assert.assertTrue("Expect file '$scriptPath' to start with UTF-8 BOM", File(scriptPath).readText().startsWith(UTF8_BOM))
        runWithKotlincAndMainKts(scriptPath, ["Hello world"])
    }

    @Test
    fun testUseSlf4j() {
        val scriptPath = "$TEST_DATA_ROOT/use-slf4j.main.kts"
        runWithKotlincAndMainKts(scriptPath, expectedErrPatterns = [".*test-slf4j"])
    }
}

fun runWithKotlincAndMainKts(
    scriptPath: String,
    expectedOutPatterns: List<String> = [],
    expectedErrPatterns: List<String> = [],
    expectedExitCode: Int = 0,
    cacheDir: Path? = null
) {
    runWithKotlinc(
        scriptPath, expectedOutPatterns, expectedErrPatterns, expectedExitCode,
        classpath = [
            ForTestCompileRuntime.mainKtsJar(),
        ],
        additionalEnvVars = [COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to (cacheDir?.toAbsolutePath()?.toString() ?: "")]
    )
}

fun runWithKotlinRunner(
    scriptPath: String,
    expectedOutPatterns: List<String> = [],
    expectedErrPatterns: List<String> = [],
    expectedExitCode: Int = 0,
    cacheDir: Path? = null
) {
    runWithKotlinLauncherScript(
        "kotlin", [scriptPath], expectedOutPatterns, expectedErrPatterns, expectedExitCode,
        additionalEnvVars = [COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to (cacheDir?.toAbsolutePath()?.toString() ?: "")]
    )
}

fun runWithK2JVMCompilerAndMainKts(
    scriptPath: String,
    expectedOutPatterns: List<String> = [],
    expectedExitCode: Int = 0,
    cacheDir: Path? = null
) {
    withProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, cacheDir?.toAbsolutePath()?.toString() ?: "") {
        runWithK2JVMCompiler(
            scriptPath, expectedOutPatterns, expectedExitCode,
            classpath = [
                ForTestCompileRuntime.mainKtsJar()
            ],
            disableScriptCompilationCache = cacheDir == null
        )
    }
}

internal const val UTF8_BOM = 0xfeff.toChar().toString()

