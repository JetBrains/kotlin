/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.buildtools.provisioning.InstallationStore
import org.jetbrains.kotlin.buildtools.provisioning.LeftoverInstallationException
import org.jetbrains.kotlin.buildtools.provisioning.LeftoverPolicy
import org.jetbrains.kotlin.buildtools.provisioning.ProvisioningListener
import org.jetbrains.kotlin.gradle.targets.native.internal.unzipTo
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Installs Kotlin/Native distributions into the directory that every build on the machine shares, `~/.konan` by default.
 * Both ways the plugin gets a distribution go through it: the toolchain ([NativeVersionValueSource]) and the download at
 * configuration time (`NativeCompilerDownloader`).
 *
 * The installing itself is done by [InstallationStore]: a distribution is unpacked out of sight and appears under its name
 * in one step, complete and with the [MARKER_FILE] inside, and builds that ask for it at the same time unpack it once.
 * This class adds what is specific to Kotlin/Native: the marker name, the archive formats, and the plugin versions that
 * installed in another way and may still be at work in the same directory.
 *
 * @param unpack unpacks an archive into a directory.
 */
internal class KotlinNativeDistributionInstaller(
    private val unpack: (archive: File, destination: File) -> Unit = ::unzipTo,
) {
    /**
     * Makes sure that [distributionDir] holds a complete distribution. [archive] is only asked for by the caller that
     * unpacks; one that finds the distribution installed, or waits for another build to install it, never asks. The
     * top-level directory of the archive must have the name of [distributionDir].
     */
    fun install(distributionDir: File, archive: () -> File) {
        val store = storeOf(distributionDir)
        val id = distributionDir.name
        var attempt = 1
        while (true) {
            if (store.find(id) != null) return
            waitForOlderInstaller(distributionDir)
            removeIfEmpty(distributionDir)
            try {
                var unpackedFrom: File? = null
                store.install(id) { staging ->
                    val from = archive().also { unpackedFrom = it }
                    // "Moving" is what the first installer did, and the plugin's tests look for these lines.
                    logger.info("Moving Kotlin/Native bundle from $from to ${distributionDir.absolutePath}")
                    unpack(from, staging.directory.toFile())
                    distributionRoot(staging.directory, id)
                }
                unpackedFrom?.let { logger.info("Moved Kotlin/Native bundle from $it to ${distributionDir.absolutePath}") }
                return
            } catch (e: LeftoverInstallationException) {
                // Something is there, and it is not a complete distribution. An older installer may have started on it
                // just now, so wait for that once more. If the directory stays as it is, nobody is unpacking it, and it
                // cannot be told apart from one that another build still uses, so it is not deleted.
                if (attempt++ < ATTEMPTS) continue
                throw IllegalStateException(
                    "Cannot install Kotlin/Native compiler to '$distributionDir': the directory is not empty, " +
                            "but it is not a complete installation. An earlier installation was probably " +
                            "interrupted. Delete the directory and run the build again.",
                    e
                )
            } catch (e: FileSystemException) {
                // An older installer does not know the store's lock. One that starts while this one unpacks creates the
                // directory first, and then the store cannot rename the distribution into place. Wait for it and look again.
                if (attempt == ATTEMPTS || !distributionDir.exists()) throw e
                logger.info("Kotlin/Native distribution '$distributionDir' appeared while it was being installed, checking it", e)
                attempt++
            }
        }
    }

    /**
     * Removes the distribution in [distributionDir], so that the next [install] unpacks it again. When some of its files
     * are in use, which on Windows keeps a directory from being moved, the distribution stays as it is.
     */
    fun remove(distributionDir: File) {
        waitForOlderInstaller(distributionDir)
        try {
            storeOf(distributionDir).remove(distributionDir.name)
        } catch (e: FileSystemException) {
            logger.warn(
                "Could not remove the Kotlin/Native distribution '$distributionDir' to install it again, its files may be in use. " +
                        "The installed distribution will be used. Stop the processes that use it and run the build again " +
                        "to reinstall it.",
                e
            )
        }
    }

    /** An empty directory holds nothing to lose. Taking a lock inside the directory is one way to end up with it. */
    private fun removeIfEmpty(distributionDir: File) {
        try {
            Files.deleteIfExists(distributionDir.toPath())
        } catch (e: DirectoryNotEmptyException) {
            // the store decides about it
        }
    }

    private fun distributionRoot(unpacked: Path, id: String): Path {
        val root = unpacked.resolve(id)
        // An archive of another version has another top-level directory. A link is not accepted in its place.
        check(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            "Kotlin Native bundle dependency was used. " +
                    "Please provide the corresponding version in 'kotlin.native.version' property instead of any other ways."
        }
        return root
    }

    /**
     * Plugin versions before 2.5.20 unpack a distribution in place, into the very directory builds read it from, and
     * hold a lock on `.lock` inside it while they do. A directory without the marker can therefore be one that such an
     * installer is filling right now, and it only counts as incomplete once that installer is done.
     *
     * This only helps against an installer that is already running. One that starts later does not wait for the store;
     * [install] deals with that when the rename fails.
     */
    private fun waitForOlderInstaller(distributionDir: File) {
        // Only open the lock file, never create it: creating it would bring the directory back while the store,
        // in another build, is about to move a distribution there.
        val channel = try {
            FileChannel.open(distributionDir.toPath().resolve(OLDER_INSTALLER_LOCK_FILE), StandardOpenOption.WRITE)
        } catch (e: NoSuchFileException) {
            return
        }
        channel.use {
            while (true) {
                try {
                    it.lock().release()
                    return
                } catch (e: OverlappingFileLockException) {
                    // an older plugin version loaded in this very JVM holds it
                    Thread.sleep(OLDER_INSTALLER_POLL_MILLIS)
                }
            }
        }
    }

    private fun storeOf(distributionDir: File) = InstallationStore(
        distributionDir.absoluteFile.parentFile.toPath(),
        markerFileName = MARKER_FILE,
        leftoverPolicy = LeftoverPolicy.FAIL,
        listener = LoggingListener,
    )

    private object LoggingListener : ProvisioningListener {
        override fun waitingForLock(id: String, lockFile: Path, holder: String?) {
            logger.lifecycle("Waiting for ${holder?.let { "process $it" } ?: "another build"} to finish installing Kotlin/Native '$id'")
        }
    }

    companion object {
        /** Marks a distribution as complete. It is the last file a distribution gets, whichever plugin version installs it. */
        const val MARKER_FILE = "provisioned.ok"

        private const val OLDER_INSTALLER_LOCK_FILE = ".lock"
        private const val OLDER_INSTALLER_POLL_MILLIS = 25L
        private const val ATTEMPTS = 3

        private val logger = Logging.getLogger(KotlinNativeDistributionInstaller::class.java)
    }
}
