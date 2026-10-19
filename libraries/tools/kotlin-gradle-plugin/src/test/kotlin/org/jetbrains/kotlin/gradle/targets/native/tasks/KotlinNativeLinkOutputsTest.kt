/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class KotlinNativeLinkOutputsTest {

    private val dir = File("/build/bin")

    private fun outputs(
        kind: CompilerOutputKind,
        target: KonanTarget,
        baseName: String = "foo",
        debuggable: Boolean = true,
        isStaticFramework: Boolean = false,
        freeCompilerArgs: List<String> = emptyList(),
    ) = KotlinNativeLinkOutputs(dir, kind, target, baseName, debuggable, isStaticFramework, freeCompilerArgs)

    private fun name(file: File) = file.relativeTo(dir).path

    private fun names(files: List<File>) = files.map { name(it) }

    @Test
    fun `executable name`() {
        assertEquals("foo.kexe", name(outputs(CompilerOutputKind.PROGRAM, KonanTarget.LINUX_X64).mainOutput))
        assertEquals("foo.exe", name(outputs(CompilerOutputKind.PROGRAM, KonanTarget.MINGW_X64).mainOutput))
    }

    @Test
    fun `library names`() {
        assertEquals("libfoo.so", name(outputs(CompilerOutputKind.DYNAMIC, KonanTarget.LINUX_X64).mainOutput))
        assertEquals("libfoo.dylib", name(outputs(CompilerOutputKind.DYNAMIC, KonanTarget.MACOS_ARM64).mainOutput))
        assertEquals("foo.dll", name(outputs(CompilerOutputKind.DYNAMIC, KonanTarget.MINGW_X64).mainOutput))
        assertEquals("libfoo.a", name(outputs(CompilerOutputKind.STATIC, KonanTarget.LINUX_X64).mainOutput))
    }

    @Test
    fun `framework name`() {
        assertEquals("foo.framework", name(outputs(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64).mainOutput))
    }

    @Test
    fun `dashes are replaced in framework and library names but kept for executables`() {
        assertEquals("my_lib.framework", name(outputs(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64, "my-lib").mainOutput))
        assertEquals("libmy_lib.so", name(outputs(CompilerOutputKind.DYNAMIC, KonanTarget.LINUX_X64, "my-lib").mainOutput))
        assertEquals("my-lib.kexe", name(outputs(CompilerOutputKind.PROGRAM, KonanTarget.LINUX_X64, "my-lib").mainOutput))
    }

    @Test
    fun `executable outputs`() {
        val linux = outputs(CompilerOutputKind.PROGRAM, KonanTarget.LINUX_X64)
        assertEquals(listOf("foo.kexe"), names(linux.files))
        assertEquals(emptyList(), names(linux.directories))

        val apple = outputs(CompilerOutputKind.PROGRAM, KonanTarget.IOS_SIMULATOR_ARM64)
        assertEquals(listOf("foo.kexe"), names(apple.files))
        assertEquals(listOf("foo.kexe.dSYM"), names(apple.directories))
    }

    @Test
    fun `library outputs`() {
        val shared = outputs(CompilerOutputKind.DYNAMIC, KonanTarget.LINUX_X64)
        assertEquals(listOf("libfoo.so", "libfoo_api.h"), names(shared.files))
        assertEquals(emptyList(), names(shared.directories))

        val static = outputs(CompilerOutputKind.STATIC, KonanTarget.LINUX_X64)
        assertEquals(listOf("libfoo.a", "libfoo_api.h"), names(static.files))

        val mingw = outputs(CompilerOutputKind.DYNAMIC, KonanTarget.MINGW_X64)
        assertEquals(listOf("foo.dll", "foo_api.h", "foo.def"), names(mingw.files))

        val appleShared = outputs(CompilerOutputKind.DYNAMIC, KonanTarget.MACOS_ARM64)
        assertEquals(listOf("libfoo.dylib.dSYM"), names(appleShared.directories))

        val appleStatic = outputs(CompilerOutputKind.STATIC, KonanTarget.MACOS_ARM64)
        assertEquals(emptyList(), names(appleStatic.directories))
    }

    @Test
    fun `framework outputs`() {
        val dynamic = outputs(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64)
        assertEquals(emptyList(), names(dynamic.files))
        assertEquals(listOf("foo.framework", "foo.framework.dSYM"), names(dynamic.directories))

        val static = outputs(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64, isStaticFramework = true)
        assertEquals(listOf("foo.framework"), names(static.directories))

        val headersOnly =
            outputs(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64, freeCompilerArgs = listOf("-Xomit-framework-binary"))
        assertEquals(listOf("foo.framework"), names(headersOnly.directories))
    }

    @Test
    fun `release apple binaries get a dSYM unless light debug is disabled`() {
        val release = outputs(CompilerOutputKind.PROGRAM, KonanTarget.IOS_ARM64, debuggable = false)
        assertEquals(listOf("foo.kexe.dSYM"), names(release.directories))

        val noLightDebug = listOf("-Xadd-light-debug=disable")
        val releaseNoLightDebug =
            outputs(CompilerOutputKind.PROGRAM, KonanTarget.IOS_ARM64, debuggable = false, freeCompilerArgs = noLightDebug)
        assertEquals(emptyList(), names(releaseNoLightDebug.directories))

        val debugNoLightDebug =
            outputs(CompilerOutputKind.PROGRAM, KonanTarget.IOS_ARM64, debuggable = true, freeCompilerArgs = noLightDebug)
        assertEquals(listOf("foo.kexe.dSYM"), names(debugNoLightDebug.directories))

        val releaseFramework = outputs(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64, debuggable = false)
        assertEquals(listOf("foo.framework", "foo.framework.dSYM"), names(releaseFramework.directories))
    }
}
