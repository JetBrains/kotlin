/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.exec.Command
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
 * Apple targets on a Linux host, with the real `konan.properties`: they are only available to build caches, only when
 * the distribution has their runtime, and without Xcode. Other hosts without Xcode cannot build their caches.
 */
@Isolated // to prevent system properties conflicts
class AppleCachesOnNonMacHostTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val konanHome: File
        get() = temporaryDirectory.resolve("konan-home")

    @Test
    fun appleTargetsAreUnavailableWithoutTheirRuntime() = onLinuxHost {
        val platformManager = platformManager()

        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.LIBRARY, KonanTarget.IOS_ARM64) }
        val error = assertUnavailable(platformManager, CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_ARM64)
        assertTrue("its runtime, 'konan/targets/ios_arm64'" in error.message!!, error.message)
        assertThrows<NoSuchElementException> { platformManager.platform(KonanTarget.IOS_ARM64) }
    }

    @Test
    fun appleTargetWithItsRuntimeCanOnlyProduceCaches() = onLinuxHost {
        withIosArm64Runtime()
        val platformManager = platformManager()

        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_ARM64) }
        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.HEADER_CACHE, KonanTarget.IOS_ARM64) }
        assertUnavailable(platformManager, CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64)
        assertUnavailable(platformManager, CompilerOutputKind.PROGRAM, KonanTarget.IOS_ARM64)
        assertUnavailable(platformManager, CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_SIMULATOR_ARM64)
        assertFalse(platformManager.isEnabled(KonanTarget.IOS_ARM64))
        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.PROGRAM, KonanTarget.LINUX_X64) }
    }

    @Test
    fun appleTargetsCannotProduceCachesOnWindows() = onHost("Windows 10") {
        withIosArm64Runtime()

        val error = assertUnavailable(platformManager(), CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_ARM64)
        assertFalse("konan/targets" in error.message!!, error.message)
    }

    @Test
    fun appleTargetHasNoXcodePartsOnANonMacHost() = onLinuxHost {
        withIosArm64Runtime()
        val configurables = platformManager().loader(KonanTarget.IOS_ARM64) as AppleConfigurables

        assertFalse(configurables.hasXcode)
        for (part in listOf({ configurables.absoluteTargetSysRoot }, { configurables.absoluteTargetToolchain })) {
            val error = assertThrows<IllegalStateException> { part() }
            assertTrue("a linux_x64 host does not have" in error.message!!, error.message)
        }
    }

    @Test
    fun clangGetsNoSysRootOrToolchainWithoutXcode() = onLinuxHost {
        withIosArm64Runtime()
        val platform = platformManager().platform(KonanTarget.IOS_ARM64)
        val clangArgs = platform.clang.clangArgs.toList()

        assertFalse("-isysroot" in clangArgs, clangArgs.toString())
        assertFalse(clangArgs.any { it.startsWith("-B") }, clangArgs.toString())
        assertTrue("-nostdlibinc" in clangArgs, clangArgs.toString())
        assertEquals(listOf("$LLVM_HOME/bin"), platform.clang.clangPaths)
    }

    @Test
    fun cacheArchiveIsWrittenByLlvmArWithoutXcode() = onLinuxHost {
        withIosArm64Runtime()
        val platform = platformManager().platform(KonanTarget.IOS_ARM64)

        val commands = archiveCommands(platform.linker, LinkerOutputKind.STATIC_LIBRARY)

        assertEquals(1, commands.size)
        val command = commands.single().argsWithExecutable
        assertEquals("$LLVM_HOME/bin/llvm-ar", command.first())
        assertTrue("/out/libcache.a" in command, command.toString())
        assertThrows<IllegalArgumentException> { archiveCommands(platform.linker, LinkerOutputKind.EXECUTABLE) }
    }

    private fun archiveCommands(linker: LinkerFlags, kind: LinkerOutputKind): List<Command> = with(linker) {
        LinkerArguments(
            tempFiles = TempFiles(temporaryDirectory.resolve("temp").path),
            objectFiles = listOf("/out/cache.o"),
            executable = "/out/libcache.a",
            staticLibraries = emptyList(),
            dynamicLibraries = emptyList(),
            linkerArgs = emptyList(),
            optimize = false,
            debug = true,
            kind = kind,
            outputDsymBundle = "",
        ).finalLinkCommands()
    }

    private fun assertUnavailable(platformManager: PlatformManager, outputKind: CompilerOutputKind, target: KonanTarget): IllegalStateException {
        val error = assertThrows<IllegalStateException> { platformManager.checkCanProduce(outputKind, target) }
        assertTrue("is not available for output kind '$outputKind'" in error.message!!, error.message)
        return error
    }

    // The runtime that a distribution built on macOS contains for ios_arm64.
    private fun withIosArm64Runtime() {
        konanHome.resolve("konan/targets/ios_arm64/native").mkdirs()
    }

    private fun platformManager(): PlatformManager {
        File(System.getProperty("konanProperties")).copyTo(konanHome.resolve("konan/konan.properties"), overwrite = true)
        val distribution = Distribution(
            konanHome.path,
            // The build fills in the LLVM distribution, which the source konan.properties leaves out.
            propertyOverrides = mapOf("llvmHome.linux_x64" to LLVM_HOME),
            konanDataDir = temporaryDirectory.resolve("konan-data").path,
        )
        return PlatformManager(distribution)
    }

    private fun onLinuxHost(block: () -> Unit) = onHost("Linux", block)

    private fun onHost(osName: String, block: () -> Unit) =
        withModifiedSystemProperties("os.name" to osName, "os.arch" to "amd64", block = block)

    private companion object {
        const val LLVM_HOME = "/kotlin-native/llvm"
    }
}
