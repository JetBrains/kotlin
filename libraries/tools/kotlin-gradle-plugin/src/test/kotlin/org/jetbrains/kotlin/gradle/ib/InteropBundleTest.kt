/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ib

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InteropBundleTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `putting klib file into target`() {
        val interopBundle = InteropBundle(tmp.root)
        interopBundle.resolve(KonanTarget.MACOS_X64, "macos.klib").createParentDirectories().writeText("macos")
        interopBundle.resolve(KonanTarget.LINUX_X64, "linux.klib").createParentDirectories().writeText("linux")

        assertEquals("macos", interopBundle.listLibraries(KonanTarget.MACOS_X64).single().readText())
        assertEquals("linux", interopBundle.listLibraries(KonanTarget.LINUX_X64).single().readText())

        assertEquals(setOf("macos.klib", "linux.klib"), interopBundle.listLibraries().map { it.name }.toSet())
    }

    @Test
    fun `putting txt file into target`() {
        val interopBundle = InteropBundle(tmp.root)
        interopBundle.resolve(KonanTarget.LINUX_X64, "linux.txt").createParentDirectories().writeText("Text")

        assertTrue(interopBundle.listLibraries(KonanTarget.LINUX_X64).isEmpty(), "Expected no library file in bundle")
        assertTrue(interopBundle.listLibraries().isEmpty(), "Expected no libray file in bundle")
    }
}

private fun File.createParentDirectories(): File = apply {
    parentFile.mkdirs()
}
