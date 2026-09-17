/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompiler
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File

@Tag("caches")
@EnforcedHostTarget
class NativeStaticCacheSanityTest : AbstractNativeSimpleTest() {

    // KT-89383
    @Test
    fun absoluteLibraryPathIsAcceptedWhenCachingLibrary() = doTestLibraryPathIsAcceptedWhenCachingLibrary(useRelativePath = false)

    // KT-89383
    @Test
    fun relativeLibraryPathIsAcceptedWhenCachingLibrary() = doTestLibraryPathIsAcceptedWhenCachingLibrary(useRelativePath = true)

    // KT-89383
    private fun doTestLibraryPathIsAcceptedWhenCachingLibrary(useRelativePath: Boolean) {
        assumeHostTargetIsCacheable()

        val sourceFile = createFile("lib.kt") {
            """
                fun hello() = "hi"
            """.trimIndent()
        }

        val klibFile = createFile("lib.klib")
        val cacheDir = createDir("cache")

        // Compile a library.
        callNativeCompiler(
            sourceFile.absolutePath,
            "-p", "library",
            "-o", klibFile.absolutePath,
        )

        // Prepare a cache for user lib using a relative path.
        callNativeCompiler(
            "-g",
            "-p", "static_cache",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${if (useRelativePath) klibFile.relativeTo(userDir) else klibFile}"
        )
    }

    private val systemCacheDir: File by lazy {
        testRunSettings.get<KotlinNativeHome>().librariesDir.resolve("cache").listFiles().orEmpty().filter {
            it.isDirectory &&
                    it.name.startsWith(HostManager.host.name) &&
                    "-g" in it.name &&
                    "STATIC" in it.name
        }.minByOrNull { it.name.length } ?: fail { "System cache directory not found" }
    }

    private val userDir: File by lazy {
        File(System.getProperty("user.dir"))
    }

    private fun createFile(name: String, contents: (() -> String)? = null): File {
        val file = buildDir.resolve(name)
        if (contents != null) file.writeText(contents.invoke()) else file.createNewFile()
        return file
    }

    private fun createDir(name: String): File {
        return buildDir.resolve(name).also { it.mkdirs() }
    }

    private fun callNativeCompiler(vararg args: String?) {
        val result = callCompiler(
            compilerArgs = args.filterNotNull().toTypedArray(),
            kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        )

        assertEquals(ExitCode.OK, result.exitCode) {
            buildString {
                appendLine("Compilation failed with exit code = ${result.exitCode}")
                appendLine("Command-line arguments: ${args.joinToString(" ")}")
                appendLine("Compiler output:")
                appendLine(result.toolOutput)
            }
        }
    }

    companion object {
        private fun assumeHostTargetIsCacheable() {
            assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
        }
    }
}
