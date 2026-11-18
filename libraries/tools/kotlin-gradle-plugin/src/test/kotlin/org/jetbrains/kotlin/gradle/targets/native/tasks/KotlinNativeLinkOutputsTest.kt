/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeLinkOutputs.Layout
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class KotlinNativeLinkOutputsTest {

    private val dir = File("/build/bin")

    private fun layout(
        kind: CompilerOutputKind,
        target: KonanTarget,
        baseName: String = "foo",
        debuggable: Boolean = true,
        isStaticFramework: Boolean = false,
    ) = Layout(dir, kind, target, baseName, debuggable, isStaticFramework)

    private fun Layout.directories() = directories(emptyList())

    private fun name(file: File) = file.relativeTo(dir).path

    private fun names(files: List<File>) = files.map { name(it) }

    @Test
    fun `executable name`() {
        assertEquals("foo.kexe", name(layout(CompilerOutputKind.PROGRAM, KonanTarget.LINUX_X64).mainOutput))
        assertEquals("foo.exe", name(layout(CompilerOutputKind.PROGRAM, KonanTarget.MINGW_X64).mainOutput))
    }

    @Test
    fun `library names`() {
        assertEquals("libfoo.so", name(layout(CompilerOutputKind.DYNAMIC, KonanTarget.LINUX_X64).mainOutput))
        assertEquals("libfoo.dylib", name(layout(CompilerOutputKind.DYNAMIC, KonanTarget.MACOS_ARM64).mainOutput))
        assertEquals("foo.dll", name(layout(CompilerOutputKind.DYNAMIC, KonanTarget.MINGW_X64).mainOutput))
        assertEquals("libfoo.a", name(layout(CompilerOutputKind.STATIC, KonanTarget.LINUX_X64).mainOutput))
    }

    @Test
    fun `framework name`() {
        assertEquals("foo.framework", name(layout(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64).mainOutput))
    }

    @Test
    fun `dashes are replaced in framework and library names but kept for executables`() {
        assertEquals("my_lib.framework", name(layout(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64, "my-lib").mainOutput))
        assertEquals("libmy_lib.so", name(layout(CompilerOutputKind.DYNAMIC, KonanTarget.LINUX_X64, "my-lib").mainOutput))
        assertEquals("my-lib.kexe", name(layout(CompilerOutputKind.PROGRAM, KonanTarget.LINUX_X64, "my-lib").mainOutput))
    }

    @Test
    fun `executable outputs`() {
        val linux = layout(CompilerOutputKind.PROGRAM, KonanTarget.LINUX_X64)
        assertEquals(listOf("foo.kexe"), names(linux.files))
        assertEquals(emptyList(), names(linux.directories()))

        val apple = layout(CompilerOutputKind.PROGRAM, KonanTarget.IOS_SIMULATOR_ARM64)
        assertEquals(listOf("foo.kexe"), names(apple.files))
        assertEquals(listOf("foo.kexe.dSYM"), names(apple.directories()))
    }

    @Test
    fun `library outputs`() {
        val shared = layout(CompilerOutputKind.DYNAMIC, KonanTarget.LINUX_X64)
        assertEquals(listOf("libfoo.so", "libfoo_api.h"), names(shared.files))
        assertEquals(emptyList(), names(shared.directories()))

        val static = layout(CompilerOutputKind.STATIC, KonanTarget.LINUX_X64)
        assertEquals(listOf("libfoo.a", "libfoo_api.h"), names(static.files))

        val mingw = layout(CompilerOutputKind.DYNAMIC, KonanTarget.MINGW_X64)
        assertEquals(listOf("foo.dll", "foo_api.h", "foo.def"), names(mingw.files))

        val appleShared = layout(CompilerOutputKind.DYNAMIC, KonanTarget.MACOS_ARM64)
        assertEquals(listOf("libfoo.dylib.dSYM"), names(appleShared.directories()))

        val appleStatic = layout(CompilerOutputKind.STATIC, KonanTarget.MACOS_ARM64)
        assertEquals(emptyList(), names(appleStatic.directories()))
    }

    @Test
    fun `framework outputs`() {
        val framework = layout(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64)
        assertEquals(emptyList(), names(framework.files))
        assertEquals(listOf("foo.framework", "foo.framework.dSYM"), names(framework.directories()))
        assertEquals(listOf("foo.framework"), names(framework.directories(listOf("-Xomit-framework-binary"))))

        val static = layout(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64, isStaticFramework = true)
        assertEquals(listOf("foo.framework"), names(static.directories()))
        assertEquals(listOf("foo.framework"), names(framework.directories(listOf("-Xstatic-framework"))))
    }

    @Test
    fun `release apple binaries get a dSYM unless light debug is disabled`() {
        val release = layout(CompilerOutputKind.PROGRAM, KonanTarget.IOS_ARM64, debuggable = false)
        assertEquals(listOf("foo.kexe.dSYM"), names(release.directories()))
        assertEquals(emptyList(), names(release.directories(listOf("-Xadd-light-debug=disable"))))

        val debug = layout(CompilerOutputKind.PROGRAM, KonanTarget.IOS_ARM64, debuggable = true)
        assertEquals(listOf("foo.kexe.dSYM"), names(debug.directories(listOf("-Xadd-light-debug=disable"))))

        val releaseFramework = layout(CompilerOutputKind.FRAMEWORK, KonanTarget.IOS_ARM64, debuggable = false)
        assertEquals(listOf("foo.framework", "foo.framework.dSYM"), names(releaseFramework.directories()))
    }

    @Test
    fun `debug flags in free compiler args are honoured like the compiler does`() {
        val release = layout(CompilerOutputKind.PROGRAM, KonanTarget.IOS_ARM64, debuggable = false)
        val dsym = listOf("foo.kexe.dSYM")

        // -g turns a release link into a debug one
        assertEquals(dsym, names(release.directories(listOf("-Xadd-light-debug=disable", "-g"))))
        // the last light debug flag wins
        assertEquals(dsym, names(release.directories(listOf("-Xadd-light-debug=disable", "-Xadd-light-debug=enable"))))
        assertEquals(emptyList(), names(release.directories(listOf("-Xadd-light-debug=enable", "-Xadd-light-debug=disable"))))
    }
}
