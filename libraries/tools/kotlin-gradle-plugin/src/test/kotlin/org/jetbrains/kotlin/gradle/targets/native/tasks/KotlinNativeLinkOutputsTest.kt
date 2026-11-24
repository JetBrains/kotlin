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
    ) = KotlinNativeLinkOutputs(dir, kind, target, baseName)

    private fun name(file: File) = file.relativeTo(dir).path

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
}
