/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import org.jetbrains.kotlin.gradle.targets.native.internal.unzipTo
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class KotlinNativeDistributionInstallerTest {

    @TempDir
    lateinit var tempDir: File

    private val konanDir: File get() = tempDir.resolve("konan")
    private val distributionDir: File get() = konanDir.resolve(DISTRIBUTION)
    private val marker: File get() = distributionDir.resolve(KotlinNativeDistributionInstaller.MARKER_FILE)

    private val installer = KotlinNativeDistributionInstaller()

    @Test
    fun `installs a distribution with its marker`() {
        val archive = createTarGz(DISTRIBUTION, "konan.properties" to "new")

        installer.install(distributionDir) { archive }

        assertEquals("new", distributionDir.resolve("konan.properties").readText())
        assertTrue(marker.exists())
        assertEquals(listOf(DISTRIBUTION), konanDir.list().orEmpty().filterNot { it.startsWith(".") }, "nothing else is left next to it")
    }

    @Test
    fun `does not ask for the archive of an installed distribution`() {
        installer.install(distributionDir) { createTarGz(DISTRIBUTION, "konan.properties" to "new") }

        installer.install(distributionDir) { fail("the distribution is installed already") }
    }

    /**
     * Such a directory may be all that is left of an installation that was interrupted, or a distribution that a build
     * with an older plugin version is running from. Unpacking over it, or deleting it, would break that build (KT-86251).
     */
    @Test
    fun `a directory that is not a complete distribution fails the installation and stays as it was`() {
        distributionDir.mkdirs()
        distributionDir.resolve("konan.properties").writeText("stale")

        val failure = assertFailsWith<IllegalStateException> {
            installer.install(distributionDir) { fail("nothing is unpacked when it cannot be installed") }
        }

        assertEquals(
            "Cannot install Kotlin/Native compiler to '$distributionDir': the directory is not empty, " +
                    "but it is not a complete installation. An earlier installation was probably interrupted. " +
                    "Delete the directory and run the build again.",
            failure.message
        )
        assertEquals(listOf("konan.properties"), distributionDir.list().orEmpty().toList())
    }

    @Test
    fun `installs into an empty directory`() {
        distributionDir.mkdirs()

        installer.install(distributionDir) { createTarGz(DISTRIBUTION, "konan.properties" to "new") }

        assertEquals("new", distributionDir.resolve("konan.properties").readText())
        assertTrue(marker.exists())
    }

    /**
     * Projects configured in parallel, with Isolated Projects for example, all ask for the distribution at the same
     * moment (KT-91499, KTI-3755).
     */
    @Test
    fun `projects installing the same distribution at once unpack it only once`() {
        val archive = createTarGz(DISTRIBUTION, "konan.properties" to "new")
        repeat(ROUNDS) { round ->
            val distribution = konanDir.resolve("$DISTRIBUTION-$round")
            val unpackCount = AtomicInteger()
            val installer = KotlinNativeDistributionInstaller(
                unpack = { _, destination ->
                    unpackCount.incrementAndGet()
                    destination.resolve(distribution.name).mkdirs()
                }
            )
            val startTogether = CyclicBarrier(PROJECTS)
            val pool = Executors.newFixedThreadPool(PROJECTS)
            try {
                val installed = List(PROJECTS) {
                    pool.submit(Callable {
                        startTogether.await()
                        runCatching { installer.install(distribution) { archive } }
                    })
                }.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }

                assertEquals(emptyList(), installed.mapNotNull { it.exceptionOrNull() }, "Round $round")
                assertEquals(1, unpackCount.get(), "Round $round: the projects that wait find the distribution installed")
                assertTrue(distribution.resolve(KotlinNativeDistributionInstaller.MARKER_FILE).exists(), "Round $round")
            } finally {
                pool.shutdownNow()
            }
        }
    }

    @Test
    fun `an archive of another version installs nothing`() {
        val archive = createTarGz("kotlin-native-prebuilt-test-0.9", "konan.properties" to "other")

        val failure = assertFailsWith<IllegalStateException> { installer.install(distributionDir) { archive } }

        assertEquals(
            "Kotlin Native bundle dependency was used. " +
                    "Please provide the corresponding version in 'kotlin.native.version' property instead of any other ways.",
            failure.message
        )
        assertEquals(emptyList(), konanDir.list().orEmpty().filterNot { it.startsWith(".") })
    }

    @Test
    fun `a removed distribution is installed again`() {
        installer.install(distributionDir) { createTarGz(DISTRIBUTION, "konan.properties" to "old") }

        installer.remove(distributionDir)

        assertFalse(distributionDir.exists())
        installer.install(distributionDir) { createTarGz(DISTRIBUTION, "konan.properties" to "new") }
        assertEquals("new", distributionDir.resolve("konan.properties").readText())
    }

    @Test
    fun `removing a distribution that is not installed does nothing`() {
        installer.remove(distributionDir)

        assertFalse(distributionDir.exists())
    }

    /**
     * Plugin versions before 2.5.20 unpack in place while they hold `.lock` in the distribution directory. Until such an
     * installer has written the marker, the directory looks like a leftover, and it must not be deleted under its hands.
     */
    @Test
    fun `waits for an installer of an older plugin version and uses what it installed`() {
        val olderInstallerUnpacking = CountDownLatch(1)
        val olderInstallerMayFinish = CountDownLatch(1)
        val olderInstaller = thread {
            KotlinInterprocessDirectoryLock(distributionDir).withLock {
                distributionDir.resolve("konan.properties").writeText("older")
                olderInstallerUnpacking.countDown()
                olderInstallerMayFinish.await()
                marker.createNewFile()
            }
        }
        val executor = Executors.newSingleThreadExecutor()
        try {
            assertTrue(olderInstallerUnpacking.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            var installing: Thread? = null
            val started = CountDownLatch(1)
            val installed = executor.submit {
                installing = Thread.currentThread()
                started.countDown()
                installer.install(distributionDir) { fail("the older installer provides the distribution") }
            }
            assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            awaitWaitingForOlderInstaller(installing!!)

            olderInstallerMayFinish.countDown()

            installed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            assertEquals("older", distributionDir.resolve("konan.properties").readText())
        } finally {
            olderInstallerMayFinish.countDown()
            executor.shutdownNow()
            olderInstaller.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
        }
    }

    /**
     * An older installer does not wait for this one. When it starts while the distribution is being unpacked, the
     * directory is there before the unpacked distribution can be moved into place.
     */
    @Test
    fun `keeps a distribution that an older installer put in place meanwhile`() {
        val archivesAskedFor = AtomicInteger()
        val installer = KotlinNativeDistributionInstaller(
            unpack = { archive, destination ->
                distributionDir.mkdirs()
                distributionDir.resolve("konan.properties").writeText("older")
                marker.createNewFile()
                unzipTo(archive, destination)
            }
        )

        installer.install(distributionDir) {
            archivesAskedFor.incrementAndGet()
            createTarGz(DISTRIBUTION, "konan.properties" to "new")
        }

        assertEquals("older", distributionDir.resolve("konan.properties").readText())
        assertEquals(1, archivesAskedFor.get(), "the second look finds the distribution and unpacks nothing")
    }

    /** A thread that waits for the older installer's lock tells nobody, so this looks at where it is; it spins without sleeping. */
    private fun awaitWaitingForOlderInstaller(thread: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (thread.stackTrace.none { it.methodName == "waitForOlderInstaller" }) {
            check(thread.isAlive) { "the thread ended instead of waiting for the older installer" }
            check(System.nanoTime() < deadline) { "the thread did not start waiting for the older installer" }
            Thread.yield()
        }
    }

    private fun createTarGz(rootDirectory: String, vararg files: Pair<String, String>): File {
        val archive = File.createTempFile("distribution", ".tar.gz", tempDir)
        TarArchiveOutputStream(GZIPOutputStream(archive.outputStream().buffered())).use { tar ->
            tar.putArchiveEntry(TarArchiveEntry("$rootDirectory/"))
            tar.closeArchiveEntry()
            for ((name, content) in files) {
                val bytes = content.toByteArray()
                tar.putArchiveEntry(TarArchiveEntry("$rootDirectory/$name").apply { size = bytes.size.toLong() })
                tar.write(bytes)
                tar.closeArchiveEntry()
            }
        }
        return archive
    }

    private companion object {
        const val DISTRIBUTION = "kotlin-native-prebuilt-test-1.0"
        const val TIMEOUT_SECONDS = 30L

        // as many projects as there were on CI when the race was noticed
        const val PROJECTS = 7
        const val ROUNDS = 50
    }
}
