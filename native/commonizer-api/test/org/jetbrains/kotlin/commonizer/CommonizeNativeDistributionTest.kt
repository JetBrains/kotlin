/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.resolveCommonizedDirectory
import org.jetbrains.kotlin.commonizer.utils.konanHome
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertTrue


public class CommonizeNativeDistributionTest {

    @get:Rule
    public val temporaryOutputDirectory: TemporaryFolder = TemporaryFolder()

    @Test
    public fun `commonize - linux platforms`() {
        val linuxTarget1 = CommonizerTarget(LINUX_X64, LINUX_ARM64)
        val linuxTarget2 = CommonizerTarget(LINUX_X64, LINUX_ARM64, LINUX_ARM32_HFP)

        CliCommonizer(this::class.java.classLoader).commonizeNativeDistribution(
            konanHome = konanHome,
            outputTargets = setOf(linuxTarget1, linuxTarget2),
            outputDirectory = temporaryOutputDirectory.root,
            logLevel = CommonizerLogLevel.Info
        )

        assertTrue(
            resolveCommonizedDirectory(temporaryOutputDirectory.root, linuxTarget1).isDirectory,
            "Expected directory for $linuxTarget1"
        )

        assertTrue(
            resolveCommonizedDirectory(temporaryOutputDirectory.root, linuxTarget2).isDirectory,
            "Expected directory for $linuxTarget2"
        )
    }

    @Test
    public fun `commonize - unix platforms`() {
        val unixTarget = CommonizerTarget(
            LINUX_X64, LINUX_ARM64,
            MACOS_X64, MACOS_ARM64,
            IOS_X64, IOS_ARM64,
            WATCHOS_ARM64, WATCHOS_ARM32, WATCHOS_X86, WATCHOS_DEVICE_ARM64,
            TVOS_ARM64, TVOS_X64
        )

        CliCommonizer(this::class.java.classLoader).commonizeNativeDistribution(
            konanHome = konanHome,
            outputTargets = setOf(unixTarget),
            outputDirectory = temporaryOutputDirectory.root,
            logLevel = CommonizerLogLevel.Info
        )

        assertTrue(
            resolveCommonizedDirectory(temporaryOutputDirectory.root, unixTarget).isDirectory,
            "Expected directory for $unixTarget"
        )
    }

    @Test
    public fun `commonize - apple platforms`() {
        assumeTrue("Test is only supported on macos", HostManager.hostIsMac)
        val iosTarget = CommonizerTarget(IOS_ARM64, IOS_X64, IOS_SIMULATOR_ARM64)
        val watchosTarget = CommonizerTarget(WATCHOS_ARM64, WATCHOS_X64, WATCHOS_SIMULATOR_ARM64, WATCHOS_DEVICE_ARM64)
        val macosTarget = CommonizerTarget(MACOS_X64, MACOS_ARM64)
        val appleTarget = SharedCommonizerTarget(iosTarget.konanTargets + watchosTarget.konanTargets + macosTarget.konanTargets)

        CliCommonizer(this::class.java.classLoader).commonizeNativeDistribution(
            konanHome = konanHome,
            outputTargets = setOf(iosTarget, watchosTarget, macosTarget, appleTarget),
            outputDirectory = temporaryOutputDirectory.root,
            logLevel = CommonizerLogLevel.Info
        )

        assertTrue(
            resolveCommonizedDirectory(temporaryOutputDirectory.root, iosTarget).isDirectory,
            "Expected directory for $iosTarget"
        )

        assertTrue(
            resolveCommonizedDirectory(temporaryOutputDirectory.root, watchosTarget).isDirectory,
            "Expected directory for $watchosTarget"
        )

        assertTrue(
            resolveCommonizedDirectory(temporaryOutputDirectory.root, macosTarget).isDirectory,
            "Expected directory for $macosTarget"
        )

        assertTrue(
            resolveCommonizedDirectory(temporaryOutputDirectory.root, appleTarget).isDirectory,
            "Expected directory for $appleTarget"
        )
    }

    @Test
    public fun `commonize - linux macos - linux macos mingw`() {
        val unixTarget = CommonizerTarget(LINUX_X64, MACOS_X64)
        val nativeTarget = CommonizerTarget(MINGW_X64, LINUX_X64, MACOS_X64)
        CliCommonizer(this::class.java.classLoader).commonizeNativeDistribution(
            konanHome = konanHome,
            outputTargets = setOf(unixTarget, nativeTarget),
            outputDirectory = temporaryOutputDirectory.root,
            logLevel = CommonizerLogLevel.Info
        )

        assertTrue(
            resolveCommonizedDirectory(temporaryOutputDirectory.root, nativeTarget).isDirectory,
            "Expected directory for $nativeTarget"
        )
    }

    @Test
    public fun `commonize - no outputTargets specified`() {
        CliCommonizer(this::class.java.classLoader).commonizeNativeDistribution(
            konanHome = konanHome,
            outputTargets = emptySet(),
            outputDirectory = temporaryOutputDirectory.root,
            logLevel = CommonizerLogLevel.Info
        )
    }
}
