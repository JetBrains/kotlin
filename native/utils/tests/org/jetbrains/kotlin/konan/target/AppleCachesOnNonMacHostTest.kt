/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Isolated
import java.io.File

/**
 * Apple targets on a Linux host, with the real `konan.properties`: they are only available to build caches,
 * and only when the properties name a toolchain for them and the distribution has their runtime.
 */
@Isolated // to prevent system properties conflicts
class AppleCachesOnNonMacHostTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val konanHome: File
        get() = temporaryDirectory.resolve("konan-home")

    @Test
    fun appleTargetsAreUnavailableWithoutAToolchain() = onLinuxHost {
        withIosArm64Runtime()
        val platformManager = platformManager()

        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.LIBRARY, KonanTarget.IOS_ARM64) }
        assertUnavailable(platformManager, CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_ARM64)
        assertThrows<NoSuchElementException> { platformManager.platform(KonanTarget.IOS_ARM64) }
    }

    @Test
    fun appleTargetWithAToolchainCanOnlyProduceCaches() = onLinuxHost {
        withIosArm64Runtime()
        val platformManager = platformManager(
            "targetToolchain.linux_x64-ios_arm64" to "/apple/toolchain",
            "targetSysRoot.ios_arm64" to "/apple/sdk",
        )

        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_ARM64) }
        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.HEADER_CACHE, KonanTarget.IOS_ARM64) }
        assertUnavailable(platformManager, CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64)
        assertUnavailable(platformManager, CompilerOutputKind.PROGRAM, KonanTarget.IOS_ARM64)
        assertUnavailable(platformManager, CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_SIMULATOR_ARM64)
        assertFalse(platformManager.isEnabled(KonanTarget.IOS_ARM64))
        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.PROGRAM, KonanTarget.LINUX_X64) }

        val configurables = platformManager.loader(KonanTarget.IOS_ARM64) as AppleConfigurablesImpl
        assertEquals("/apple/toolchain/usr", configurables.absoluteTargetToolchain)
        assertEquals("/apple/sdk", configurables.absoluteTargetSysRoot)
        assertFalse("/apple/toolchain" in configurables.dependencies)
        assertFalse("/apple/sdk" in configurables.dependencies)
    }

    @Test
    fun appleCachesNeedTheRuntimeFromAMacDistribution() = onLinuxHost {
        val platformManager = platformManager(
            "targetToolchain.linux_x64-ios_arm64" to "/apple/toolchain",
            "targetSysRoot.ios_arm64" to "/apple/sdk",
        )

        val error = assertThrows<IllegalStateException> {
            platformManager.checkCanProduce(CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_ARM64)
        }
        assertTrue("'konan/targets/ios_arm64'" in error.message!!, error.message)
    }

    @Test
    fun appleSysRootMustBeConfiguredWithTheToolchain() = onLinuxHost {
        val platformManager = platformManager("targetToolchain.linux_x64-ios_arm64" to "/apple/toolchain")

        val configurables = platformManager.loader(KonanTarget.IOS_ARM64) as AppleConfigurablesImpl
        val error = assertThrows<IllegalStateException> { configurables.absoluteTargetSysRoot }
        assertTrue("targetSysRoot.ios_arm64" in error.message!!, error.message)
    }

    private fun assertUnavailable(platformManager: PlatformManager, outputKind: CompilerOutputKind, target: KonanTarget) {
        val error = assertThrows<IllegalStateException> { platformManager.checkCanProduce(outputKind, target) }
        assertTrue("is not available for output kind '$outputKind'" in error.message!!, error.message)
    }

    // The runtime that a distribution built on macOS contains for ios_arm64.
    private fun withIosArm64Runtime() {
        konanHome.resolve("konan/targets/ios_arm64/native").mkdirs()
    }

    private fun platformManager(vararg propertyOverrides: Pair<String, String>): PlatformManager {
        File(System.getProperty("konanProperties")).copyTo(konanHome.resolve("konan/konan.properties"), overwrite = true)
        val distribution = Distribution(
            konanHome.path,
            propertyOverrides = propertyOverrides.toMap(),
            konanDataDir = temporaryDirectory.resolve("konan-data").path,
        )
        return PlatformManager(distribution)
    }

    private fun onLinuxHost(block: () -> Unit) =
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64", block = block)
}
