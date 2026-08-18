/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.target.InstalledXcode
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

/**
 * Provisions the whole Xcode expected by a Kotlin/Native distribution (`konan.properties`:
 * `xcodeVersion`/`xcodeBuild`/`xcodeArtifactUrl`) under `$KONAN_DATA_DIR/dependencies/xcode_<version>_<build>`, and
 * returns its `Xcode.app` directory (or `null` when it couldn't/shouldn't be provisioned). Must only be called on
 * macOS.
 *
 * This is the counterpart to the compiler's `useProvisionedXcode` resolution (see
 * [org.jetbrains.kotlin.konan.target.AppleConfigurablesImpl]): the provisioner puts the Xcode where the compiler
 * expects it. The directory name matches what the CI agent images pre-create (vm-templates `symlink-xcode.sh`), so on
 * CI this is a fast no-op.
 */
object XcodeProvisioner {

    /**
     * 1. If `xcode_<version>_<build>` already exists (a real directory, or a symlink the agent image pre-created),
     *    it is used as-is — nothing is downloaded.
     * 2. Otherwise, if the currently selected Xcode already has the expected [build], a symlink to it is created
     *    (no internal server needed — the default on a developer machine that happens to have the right Xcode).
     * 3. Otherwise a full-Xcode download is required:
     *    - if [isTeamCity] is `true` → fail loudly (agents must ship the expected Xcode via their image), whether or
     *      not the internal server is enabled: a CI build must never spend time downloading a whole Xcode;
     *    - if [serverEnabled] is `false` → return `null`, leaving the decision to the caller (the build side turns
     *      this into an actionable "install/select that Xcode" error);
     *    - otherwise download the `.xip` at [artifactUrl], expand it, and check that it really is the expected build.
     *
     * The download step goes through [DependencyProcessor], so it shares the same file lock, retrying downloader and
     * idempotency marker as every other Kotlin/Native dependency — safe under concurrent Gradle tasks/processes.
     */
    fun provisionXcode(
        konanDataDir: String?,
        version: String,
        build: String,
        artifactUrl: String,
        serverEnabled: Boolean,
        isTeamCity: Boolean,
    ): File? {
        check(HostManager.hostIsMac) { "Xcode can only be provisioned on macOS hosts." }

        val dependenciesRoot = DependencyDirectories.getDependenciesRoot(konanDataDir)
        val dependency = "xcode_${version}_${build}"
        val target = dependenciesRoot.resolve(dependency)
        val targetPath = target.toPath()
        // `exists()` follows symlinks, so a valid symlink or a real directory short-circuits here — but it is still
        // validated, see [checkProvisionedBuild].
        if (Files.exists(targetPath)) {
            checkProvisionedBuild(target, version, build)
            return target
        }
        // A dangling symlink from a previously removed Xcode: drop it before recreating.
        if (Files.isSymbolicLink(targetPath)) Files.delete(targetPath)
        target.parentFile.mkdirs()

        // Reuse the currently selected Xcode when its build matches the expected one (no internal server needed).
        val current = InstalledXcode()
        val currentApp = current.xcodeApp
        if (currentApp != null && currentBuildOf(current) == build) {
            println("[whole-xcode] Current Xcode build $build matches; symlinking $currentApp -> $target")
            try {
                Files.createSymbolicLink(targetPath, currentApp.toPath())
            } catch (e: FileAlreadyExistsException) {
                checkProvisionedBuild(target, version, build)
                // Another process created it concurrently; use it if it fits the requirements
            }
            return target
        }

        if (isTeamCity) {
            error(
                "Expected Xcode $version (build $build) is not installed on this TeamCity agent " +
                        "(current selected Xcode build: ${currentXcodeBuild() ?: "unknown"}). " +
                        "Downloading the full Xcode during a TeamCity build is not allowed; " +
                        "update the agent image to Xcode $build."
            )
        }
        if (!serverEnabled) return null

        println("[whole-xcode] Provisioning Xcode $version ($build): downloading $artifactUrl and expanding...")
        DependencyProcessor(
            dependenciesRoot = dependenciesRoot,
            dependenciesUrl = artifactUrl, // unused for a Remote.Url candidate, but the ctor requires a value.
            dependencyToCandidates = mapOf(dependency to listOf(DependencySource.Remote.Url(artifactUrl))),
            archiveType = ArchiveType.XIP,
            customProgressCallback = xcodeDownloadProgress(),
        ).run()
        checkProvisionedBuild(target, version, build)
        return target
    }

    // The directory name is the only thing tying a provisioned Xcode to a build, so check that the bundle really is
    // that build: a mistyped `xcodeArtifactUrl`, a stale CI agent image or a hand-made symlink would otherwise point
    // the entire build at the wrong toolchain silently.
    //
    // This covers a pre-existing Xcode as well as a freshly downloaded one, and not just for the agent-image case:
    // `DependencyProcessor` marks a dependency extracted before this runs, so checking only after a download would
    // reject a bad artifact once and then silently accept it on every later build, the directory now existing.
    private fun checkProvisionedBuild(target: File, version: String, build: String) {
        val actual = currentBuildOf(InstalledXcode(developerDir = target.resolve("Contents/Developer").path))
        check(actual == build) {
            "The Xcode provisioned at $target has ProductBuildVersion '${actual ?: "unknown"}', but Xcode $version " +
                    "(build $build) was expected. Check 'xcodeArtifactUrl' in konan.properties, or remove $target " +
                    "so that it is provisioned again."
        }
    }

    // Reports the download progress of the (large) Xcode archive, deduplicated to whole-percent steps to avoid spam.
    private fun xcodeDownloadProgress(): ProgressCallback {
        var lastPercent = -1
        return { _, currentBytes, totalBytes ->
            if (totalBytes > 0) {
                val percent = (currentBytes * 100 / totalBytes).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    println("[whole-xcode] Downloading Xcode: $percent% (${currentBytes / 1_000_000} / ${totalBytes / 1_000_000} MB)")
                }
            }
        }
    }

    private fun currentXcodeBuild(): String? = currentBuildOf(InstalledXcode())

    private fun currentBuildOf(xcode: InstalledXcode): String? = try {
        xcode.productBuildVersion
    } catch (e: Exception) {
        null
    }
}
