/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.TempFiles
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.util.Properties

/**
 * `-dynamic-linker` used to be emitted for every linux_x64 executable, including one the user asked
 * to link statically. It is emitted ahead of `linkerArgs`, so no user flag and no
 * `-Xoverride-konan-properties` value could remove it, and the resulting binary carried a PT_INTERP
 * that made the kernel hand it to the dynamic loader.
 */
class LinkerStaticExecutableTest {

    @TempDir
    lateinit var dependenciesRoot: java.io.File

    private fun linkCommandArguments(linkerArgs: List<String>): List<String> {
        val configurables =
            loadConfigurables(KonanTarget.LINUX_X64, linuxX64Properties(), dependenciesRoot.path) { _, _, _ -> }
        return linker(configurables).run {
            LinkerArguments(
                tempFiles = TempFiles(),
                objectFiles = listOf("main.o"),
                executable = "main.kexe",
                staticLibraries = emptyList(),
                dynamicLibraries = emptyList(),
                linkerArgs = linkerArgs,
                optimize = false,
                debug = false,
                kind = LinkerOutputKind.EXECUTABLE,
                outputDsymBundle = "",
            ).finalLinkCommands()
        }.flatMap { it.argsWithExecutable }
    }

    @Test
    fun `an ordinary executable gets a dynamic interpreter`() {
        val arguments = linkCommandArguments(emptyList())
        assertTrue("-dynamic-linker" in arguments, "expected a dynamic interpreter, got: $arguments")
    }

    @Test
    fun `a static executable does not`() {
        val arguments = linkCommandArguments(listOf("-static"))
        assertFalse("-dynamic-linker" in arguments, "PT_INTERP would make it unable to start: $arguments")
        assertTrue("-static" in arguments, "the user's own flag must survive: $arguments")
    }

    private fun linuxX64Properties() = Properties().apply {
        setProperty("dependenciesUrl", "https://example.invalid/dependencies")
        // Absolute paths on purpose: DependencyProcessor.resolve() returns an absolute path
        // unchanged, so the test needs no downloaded dependencies. Leaving
        // predefinedLlvmDistributions and predefinedLibffiVersions unset keeps the dependency list
        // empty, so nothing is checked for availability either.
        setProperty("libffiDir.linux_x64", "/libffi")
        setProperty("llvmHome.linux_x64", "/llvm-home")
        setProperty("gccToolchain.linux_x64", "/gcc-toolchain")
        setProperty("targetToolchain.linux_x64-linux_x64", "/target-toolchain")
        setProperty("targetSysRoot.linux_x64", "/sysroot")
        setProperty("crtFilesLocation.linux_x64", "usr/lib")
        setProperty("libGcc.linux_x64", "/lib/gcc")
        setProperty("dynamicLinker.linux_x64", "/lib64/ld-linux-x86-64.so.2")
        setProperty("linker.linux_x64", "/ld.lld")
        setProperty("linkerKonanFlags.linux_x64", "-Bstatic -lstdc++ -Bdynamic -ldl -lm -lpthread")
        setProperty("linkerGccFlags", "-lgcc -lgcc_eh -lc")
        setProperty("ar.linux_x64", "/ar")
    }
}
