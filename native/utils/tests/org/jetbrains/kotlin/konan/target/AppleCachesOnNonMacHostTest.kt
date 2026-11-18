/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.MissingXcodeException
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

    private val llvmHome: File
        get() = temporaryDirectory.resolve("llvm")

    // Fakes the host through system properties, which HostManager reads.
    private fun withFakeHost(osName: String, block: () -> Unit) =
        withModifiedSystemProperties("os.name" to osName, "os.arch" to "amd64", block = block)

    private fun withFakeLinuxHost(block: () -> Unit) = withFakeHost("Linux", block)

    @Test
    fun appleTargetsAreUnavailableWithoutTheirRuntime() = withFakeLinuxHost {
        val platformManager = platformManager()

        assertDoesNotThrow { platformManager.checkCanProduce(CompilerOutputKind.LIBRARY, KonanTarget.IOS_ARM64) }
        val error = assertUnavailable(platformManager, CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_ARM64)
        assertTrue("its runtime, 'konan/targets/ios_arm64'" in error.message!!, error.message)
        assertThrows<NoSuchElementException> { platformManager.platform(KonanTarget.IOS_ARM64) }
    }

    @Test
    fun appleTargetWithItsRuntimeCanOnlyProduceCaches() = withFakeLinuxHost {
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
    fun appleTargetsCannotProduceCachesOnWindows() = withFakeHost("Windows 10") {
        withIosArm64Runtime()

        val error = assertUnavailable(platformManager(), CompilerOutputKind.STATIC_CACHE, KonanTarget.IOS_ARM64)
        assertFalse("konan/targets" in error.message!!, error.message)
    }

    @Test
    fun appleTargetHasNoXcodePartsOnANonMacHost() = withFakeLinuxHost {
        withIosArm64Runtime()
        val configurables = platformManager().loader(KonanTarget.IOS_ARM64) as AppleConfigurables

        for (part in listOf({ configurables.absoluteTargetSysRoot }, { configurables.absoluteTargetToolchain })) {
            val error = assertThrows<MissingXcodeException> { part() }
            assertTrue("a linux_x64 host does not have" in error.message, error.message)
        }
    }

    @Test
    fun cStubsAreCompiledWithoutXcode() = withFakeLinuxHost {
        withIosArm64Runtime()
        val clangArgs = platformManager().platform(KonanTarget.IOS_ARM64).clangForCStubs.clangArgs.toList()

        // A fixed sysroot, so that clang doesn't take one from SDKROOT.
        assertTrue(listOf("-isysroot", "/") in clangArgs.windowed(2), clangArgs.toString())
        assertTrue("-nostdlibinc" in clangArgs, clangArgs.toString())
    }

    @Test
    fun cacheArchiveNeedsLlvmLibtoolDarwinWithoutXcode() = withFakeLinuxHost {
        withIosArm64Runtime()
        val linker = platformManager().platform(KonanTarget.IOS_ARM64).linker

        val noLibtool = assertThrows<MissingXcodeException> { archiveCommands(linker, LinkerOutputKind.STATIC_LIBRARY) }
        assertTrue("-Xbinary=useLlvmLibtoolDarwin=true" in noLibtool.message, noLibtool.message)

        // Only the dev LLVM distribution has llvm-libtool-darwin.
        val missing = assertThrows<IllegalStateException> {
            archiveCommands(linker, LinkerOutputKind.STATIC_LIBRARY, useLlvmLibtoolDarwin = true)
        }
        assertTrue("-Xllvm-variant=dev" in missing.message!!, missing.message)

        val llvmLibtoolDarwin = llvmHome.resolve("bin/llvm-libtool-darwin").apply { parentFile.mkdirs(); createNewFile() }
        val command = archiveCommands(linker, LinkerOutputKind.STATIC_LIBRARY, useLlvmLibtoolDarwin = true).single()
            .argsWithExecutable
        assertEquals(llvmLibtoolDarwin.path, command.first())
        assertTrue("/out/libcache.a" in command, command.toString())

        val noLinker = assertThrows<MissingXcodeException> {
            archiveCommands(linker, LinkerOutputKind.EXECUTABLE, useLlvmLibtoolDarwin = true)
        }
        assertTrue("a linux_x64 host does not have" in noLinker.message, noLinker.message)
    }

    private fun archiveCommands(
        linker: LinkerFlags,
        kind: LinkerOutputKind,
        useLlvmLibtoolDarwin: Boolean = false,
    ): List<Command> = with(linker) {
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
            useLlvmLibtoolDarwin = useLlvmLibtoolDarwin,
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
            propertyOverrides = mapOf("llvmHome.linux_x64" to llvmHome.path),
            konanDataDir = temporaryDirectory.resolve("konan-data").path,
        )
        return PlatformManager(distribution)
    }
}
