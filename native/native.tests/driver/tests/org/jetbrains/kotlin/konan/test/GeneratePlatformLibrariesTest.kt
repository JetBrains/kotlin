/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.native.executors.RunProcessException
import org.jetbrains.kotlin.native.executors.runProcess
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@Tag("driver")
class GeneratePlatformLibrariesTest : AbstractNativeSimpleTest() {
    private val konanHome get() = testRunSettings.get<KotlinNativeHome>().dir
    private val buildDir get() = testRunSettings.get<Binaries>().testBinariesDir
    private val generatePlatform
        get() = konanHome.resolve("bin").resolve(if (HostManager.hostIsMingw) "generate-platform.bat" else "generate-platform")

    // KT-90608
    @Test
    fun stdlibCacheUsesKonanDataDir() {
        val workDir = buildDir.resolve("generatePlatformLibraries").apply {
            deleteRecursively()
            mkdirs()
        }
        // No def files, so only the standard library cache is built.
        val inputDir = workDir.resolve("input").apply { mkdirs() }
        val konanDataDir = workDir.resolve("konanData")

        // With airplaneMode, the stdlib cache compilation fails fast instead of downloading the dependencies.
        // It still creates `<konan data dir>/dependencies`, which shows which data dir it used.
        try {
            runProcess(
                generatePlatform.absolutePath,
                "-target", HostManager.host.name,
                "-input-directory", inputDir.absolutePath,
                "-output-directory", workDir.resolve("output").absolutePath,
                "-cache-directory", workDir.resolve("cache").absolutePath,
                "-cache-kind", "static_cache",
                "-Xkonan-data-dir", konanDataDir.absolutePath,
                "-cache-arg", "-Xoverride-konan-properties=airplaneMode=true",
            ) {
                timeout = 5.minutes
                environment["JAVA_HOME"] = System.getProperty("java.home")
            }
        } catch (_: RunProcessException) {
            // Expected: the dependencies aren't in the fresh data dir.
        }

        assertTrue(
            konanDataDir.resolve("dependencies").exists(),
            "The standard library cache compilation must use the data dir passed with -Xkonan-data-dir"
        )
    }
}
