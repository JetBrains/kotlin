/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts.test

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR
import org.jetbrains.kotlin.mainKts.COMPILED_SCRIPTS_CACHE_DIR_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.runAndCheckResults
import org.jetbrains.kotlin.scripting.compiler.plugin.runWithK2JVMCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.runWithKotlinLauncherScript
import org.jetbrains.kotlin.scripting.compiler.plugin.runWithKotlinc
import org.jetbrains.kotlin.testFederation.SmokeTest
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

@SmokeTest
class MainKtsIT {

    @Test
    fun testResolveJunit() {
        runWithKotlincAndMainKts("$TEST_DATA_ROOT/hello-resolve-junit.main.kts", listOf("Hello, World!"))
    }

    @Test
    fun testCompileResolveJunit() {
        withTempDir { tmpDir ->
            runWithK2JVMCompiler(
                "$TEST_DATA_ROOT/hello-resolve-junit.main.kts",
                classpath = listOf(
                    ForTestCompileRuntime.mainKtsJar()
                ),
                expectedExitCode = 0,
                skipScriptArgument = true,
                disableScriptCompilationCache = true,
                additionalArgs = listOf("-d", tmpDir.path)
            )
        }
    }

    @Test
    @Disabled // Fails on TC most likely due to repo proxying
    fun testKotlinxHtml() {
        runWithK2JVMCompilerAndMainKts(
            "$TEST_DATA_ROOT/kotlinx-html.main.kts",
            listOf("<html>", "  <body>", "    <h1>Hello, World!</h1>", "  </body>", "</html>")
        )
    }

    @Test
    fun testImport() {
        val mainKtsJar = ForTestCompileRuntime.mainKtsJar()

        runWithK2JVMCompiler(
            "$TEST_DATA_ROOT/import-test.main.kts",
            listOf("Hi from common", "Hi from middle", "Hi from main", "sharedVar == 5"),
            classpath = listOf(mainKtsJar)
        )
    }

    @Test
    fun testCompileWithImport() {
        val mainKtsJar = ForTestCompileRuntime.mainKtsJar()

        withTempDir { tmpDir ->
            runWithK2JVMCompiler(
                "$TEST_DATA_ROOT/import-test.main.kts",
                classpath = listOf(mainKtsJar),
                skipScriptArgument = true,
                additionalArgs = listOf("-d", tmpDir.absolutePath)
            )
        }
    }

    @Test
    fun testKt86352() {
        val mainKtsJar = ForTestCompileRuntime.mainKtsJar()

        runWithK2JVMCompiler(
            "$TEST_DATA_ROOT/kt86352-main.main.kts",
            listOf("result = MyData"),
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
        if (isRunningTestOnK2) {
            // test fails on K1, which may cause some disruptions, but we plan to drop it very soon anyway, so maybe it's ok
            val script = File("$TEST_DATA_ROOT/import-test.main.kts").absolutePath
            val cache = createTempDirectory("main.kts.test")

            try {
                Assertions.assertTrue(cache.exists() && cache.listDirectoryEntries("*.jar").isEmpty())
                runWithKotlincAndMainKts(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
                val cacheFile = cache.listDirectoryEntries("*.jar").firstOrNull()
                Assertions.assertTrue(cacheFile != null && cacheFile.exists())

                // run generated jar with java
                val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
                val args = listOf(javaExecutable.absolutePath, "-jar", cacheFile!!.toString())
                runAndCheckResults(
                    args, OUT_FROM_IMPORT_TEST,
                    additionalEnvVars = listOf(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to cache.toAbsolutePath().toString())
                )

                // this run should use the cached script
                runWithKotlincAndMainKts(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
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
            Assertions.assertTrue(cache.exists() && cache.listDirectoryEntries("*.jar").isEmpty())
            runWithK2JVMCompilerAndMainKts(script, OUT_FROM_IMPORT_TEST, cacheDir = cache)
            val cacheFile = cache.listDirectoryEntries("*.jar").firstOrNull()
            Assertions.assertTrue(cacheFile != null && cacheFile.exists())

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
            Assertions.assertTrue(cache.exists() && cache.listDirectoryEntries("*.jar").isEmpty())
            runWithKotlinRunner(scriptPath, expectedTestOutput, cacheDir = cache)
            val cacheFile = cache.listDirectoryEntries("*.jar").firstOrNull()
            Assertions.assertTrue(cacheFile != null && cacheFile.exists())

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
        Assertions.assertTrue(File(scriptPath).readText().startsWith(UTF8_BOM), "Expect file '$scriptPath' to start with UTF-8 BOM")
        runWithKotlincAndMainKts(scriptPath, listOf("Hello world"))
    }

    @Test
    fun testUseSlf4j() {
        val scriptPath = "$TEST_DATA_ROOT/use-slf4j.main.kts"
        runWithKotlincAndMainKts(scriptPath, expectedErrPatterns = listOf(".*test-slf4j"))
    }

    @Test
    fun testWithCustomLocalRepository() {
        withTempDir("main.kts.dep") { jardir ->
            val libsrc = jardir.resolve("src.kt").apply { writeText("fun testFun() = \"hello\"") }
            runWithK2JVMCompiler(
                arrayOf("-d", jardir.resolve("lib.jar").absolutePath.toString(), libsrc.absolutePath)
            )
            val scr = jardir.resolve("s.main.kts").apply {
                writeText(
                    """
                    @file:Repository("${jardir.platformIndependentPathString()}")
                    @file:DependsOn("lib.jar")
                    println(testFun())
                """.trimIndent()
                )
            }
            val mainKtsJar = ForTestCompileRuntime.mainKtsJar()

            runWithK2JVMCompiler(
                scr.absolutePath,
                listOf("hello"),
                classpath = listOf(mainKtsJar)
            )

            runWithKotlincAndMainKts(
                scr.absolutePath,
                listOf("hello"),
            )
        }
    }

    @Test
    fun testWithDifferrentJvmTarget() {
        val jvmTarget = System.getProperty("java.runtime.version")?.substringBefore(".") ?: "11"
        withTempDir("main.kts.jvmtarget") { jardir ->
            val libsrc = jardir.resolve("src.kt").apply {
                writeText(
                    """
                    fun testFun() = "hello"
                    inline fun <T> runInline(block: () -> T): T = block()
                    """.trimIndent())
            }
            runWithKotlinc(
                arrayOf("-jvm-target", jvmTarget, "-d", jardir.resolve("lib.jar").absolutePath.toString(), libsrc.absolutePath)
            )
            fun makeScript(jvmTarget: String): String = """
                                @file:CompilerOptions("-jvm-target", "$jvmTarget")
                                @file:DependsOn("${jardir.resolve("lib.jar").absoluteFile.platformIndependentPathString()}")
                                println(runInline(::testFun))
                            """.trimIndent()

            val scrErr = jardir.resolve("serr.main.kts").apply { writeText(makeScript("1.8")) }
            val scrOk = jardir.resolve("sok.main.kts").apply { writeText(makeScript(jvmTarget)) }

            val mainKtsJar = ForTestCompileRuntime.mainKtsJar()

            runWithK2JVMCompiler(
                scrErr.absolutePath,
                expectedExitCode = 1,
                expectedSomeErrPatterns = listOf(".*cannot inline bytecode built with JVM target $jvmTarget.*"),
                classpath = listOf(mainKtsJar)
            )
            runWithK2JVMCompiler(
                scrOk.absolutePath,
                listOf("hello"),
                classpath = listOf(mainKtsJar)
            )
            runWithKotlincAndMainKts(
                scrOk.absolutePath,
                listOf("hello"),
            )
        }
    }

}

private fun File.platformIndependentPathString(): String = path.replace(File.separatorChar, '/')

internal fun <R> withTempDir(keyName: String = "tmp", body: (File) -> R): R {
    val tempDir = Files.createTempDirectory(keyName).toFile()
    try {
        return body(tempDir)
    } finally {
        tempDir.deleteRecursively()
    }
}

fun runWithKotlincAndMainKts(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedErrPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    cacheDir: Path? = null
) {
    runWithKotlinc(
        scriptPath, expectedOutPatterns, expectedErrPatterns, expectedExitCode,
        classpath = listOf(
            ForTestCompileRuntime.mainKtsJar(),
        ),
        additionalEnvVars = listOf(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to (cacheDir?.toAbsolutePath()?.toString() ?: ""))
    )
}

fun runWithKotlinRunner(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedErrPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0,
    cacheDir: Path? = null
) {
    runWithKotlinLauncherScript(
        "kotlin", listOf(scriptPath), expectedOutPatterns, expectedErrPatterns, expectedExitCode,
        additionalEnvVars = listOf(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR to (cacheDir?.toAbsolutePath()?.toString() ?: ""))
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
                ForTestCompileRuntime.mainKtsJar()
            ),
            disableScriptCompilationCache = cacheDir == null
        )
    }
}

internal const val UTF8_BOM = 0xfeff.toChar().toString()
