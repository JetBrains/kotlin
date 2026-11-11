/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import org.jetbrains.kotlin.gradle.targets.native.toolchain.NativeVersionValueSource
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the legacy configuration-time downloader, which is deprecated in favour of the toolchain
 * (see KotlinNativeInstaller). Delete these tests together with NativeCompilerDownloader.
 */
class NativeCompilerDownloaderConcurrencyTest {

    /**
     * Plugin versions without the installation lock each unpack the distribution into their own tmp directory
     * and then move it to the same place. Exactly one of them should install it, and the rest should quietly
     * accept the installed one (KTI-3755).
     */
    @Test
    fun `publishing the same distribution at once doesn't fail`(@TempDir konanDir: File) {
        repeat(ROUNDS) { round ->
            val distribution = konanDir.resolve("kotlin-native-prebuilt-$round")
            val unpacked = List(PROJECTS) { unpackedDistribution(konanDir, distribution.name) }

            val published = atOnce(unpacked.map { { NativeCompilerDownloader.publishExtractedDistribution(it, distribution) } })

            assertEquals(
                emptyList(),
                published.mapNotNull { it.exceptionOrNull() },
                "Round $round: losing the race to install the distribution should not be an error",
            )
            assertEquals(1, published.count { it.getOrThrow() }, "Round $round: exactly one project installs the distribution")
            assertTrue(NativeVersionValueSource.isProvisioned(distribution), "Round $round: the distribution is complete")
        }
    }

    @Test
    fun `projects installing the same distribution at once unpack it only once`(@TempDir konanDir: File) {
        repeat(ROUNDS) { round ->
            val distribution = konanDir.resolve("kotlin-native-prebuilt-$round")
            val unpackCount = AtomicInteger()

            val installed = atOnce(List(PROJECTS) {
                {
                    NativeCompilerDownloader.installDistribution(distribution, logger) { tmpDir ->
                        unpackCount.incrementAndGet()
                        tmpDir.resolve(distribution.name).mkdirs()
                    }
                }
            })

            assertEquals(emptyList(), installed.mapNotNull { it.exceptionOrNull() }, "Round $round")
            assertEquals(1, unpackCount.get(), "Round $round: the projects waiting for the lock should find the distribution installed")
            assertTrue(NativeVersionValueSource.isProvisioned(distribution), "Round $round: the distribution is complete")
        }
        assertEquals(
            emptyList(),
            konanDir.listFiles().orEmpty().map { it.name }.filter { it.startsWith("compiler-") },
            "No tmp directories should be left behind",
        )
    }

    @Test
    fun `publishing over an installed distribution leaves it alone`(@TempDir konanDir: File) {
        val installed = unpackedDistribution(konanDir, "kotlin-native-prebuilt").apply { resolve("konanc").writeText("installed") }
        val unpacked = unpackedDistribution(konanDir, "kotlin-native-prebuilt").apply { resolve("konanc").writeText("unpacked") }
        val distribution = konanDir.resolve("kotlin-native-prebuilt")
        NativeCompilerDownloader.publishExtractedDistribution(installed, distribution)

        assertFalse(NativeCompilerDownloader.publishExtractedDistribution(unpacked, distribution))
        assertEquals("installed", distribution.resolve("konanc").readText())
    }

    @Test
    fun `publishing over a directory that is not a distribution fails`(@TempDir konanDir: File) {
        val unpacked = unpackedDistribution(konanDir, "kotlin-native-prebuilt")
        // what the toolchain leaves there when it takes its lock and then doesn't finish the installation
        val distribution = konanDir.resolve("kotlin-native-prebuilt").apply {
            mkdirs()
            resolve(".lock").createNewFile()
        }

        assertFailsWith<FileSystemException> { NativeCompilerDownloader.publishExtractedDistribution(unpacked, distribution) }
    }

    @Test
    fun `installing over a directory that is not a distribution fails before unpacking`(@TempDir konanDir: File) {
        // what the toolchain leaves there when it takes its lock and then doesn't finish the installation
        val distribution = konanDir.resolve("kotlin-native-prebuilt").apply {
            mkdirs()
            resolve(".lock").createNewFile()
        }
        val unpackCount = AtomicInteger()

        assertFailsWith<IllegalStateException> {
            NativeCompilerDownloader.installDistribution(distribution, logger) { unpackCount.incrementAndGet() }
        }
        assertEquals(0, unpackCount.get(), "Nothing should be unpacked when it can't be installed")
        assertEquals(listOf(".lock"), distribution.list().orEmpty().toList(), "The directory should be left as it was")
    }

    /**
     * AndroidX case from KT-86251: the build installs through the downloader while a TestKit build it started
     * installs the same distribution through the toolchain, which unpacks in place under its own lock.
     */
    @Test
    fun `installing while the toolchain is unpacking in place waits for it`(@TempDir konanDir: File) {
        val distribution = konanDir.resolve("kotlin-native-prebuilt")
        val toolchainStarted = CountDownLatch(1)
        val toolchain = thread {
            KotlinInterprocessDirectoryLock(distribution).withLock {
                distribution.resolve("konanc").writeText("toolchain")
                toolchainStarted.countDown()
                Thread.sleep(500) // give the downloader time to run into the half-unpacked distribution
                NativeVersionValueSource.markAsProvisioned(distribution)
            }
        }
        toolchainStarted.await()
        val unpackCount = AtomicInteger()

        NativeCompilerDownloader.installDistribution(distribution, logger) { unpackCount.incrementAndGet() }
        toolchain.join()

        assertEquals(0, unpackCount.get(), "The downloader should not unpack what the toolchain is installing")
        assertEquals("toolchain", distribution.resolve("konanc").readText(), "The toolchain's installation should stay intact")
    }

    /** Runs [actions] in parallel, releasing all of them at the same moment. */
    private fun <T> atOnce(actions: List<() -> T>): List<Result<T>> {
        val pool = Executors.newFixedThreadPool(actions.size)
        try {
            val startTogether = CyclicBarrier(actions.size)
            return actions
                .map { action ->
                    pool.submit(Callable {
                        startTogether.await()
                        runCatching(action)
                    })
                }
                .map { it.get() }
        } finally {
            pool.shutdownNow()
        }
    }

    private fun unpackedDistribution(konanDir: File, name: String): File {
        val tmpDir = Files.createTempDirectory(konanDir.toPath(), "compiler-").toFile()
        return tmpDir.resolve(name).also {
            it.mkdirs()
            NativeVersionValueSource.markAsProvisioned(it)
        }
    }

    private companion object {
        // as many as there were on CI when the race was noticed
        const val PROJECTS = 7
        const val ROUNDS = 200

        val logger: Logger = Logging.getLogger(NativeCompilerDownloaderConcurrencyTest::class.java)
    }
}
