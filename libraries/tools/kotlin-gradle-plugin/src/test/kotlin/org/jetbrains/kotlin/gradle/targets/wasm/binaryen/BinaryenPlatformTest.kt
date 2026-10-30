/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlatform.Companion.parseBinaryenPlatformOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Checks that the detected platform matches the names of the archives published in
 * https://github.com/WebAssembly/binaryen/releases
 */
class BinaryenPlatformTest {

    @Test
    fun `windows on x86_64`() {
        assertEquals("x86_64-windows", parseBinaryenPlatformOrNull("Windows 11", "amd64")?.platform)
    }

    @Test
    fun `windows on arm reports arm64 despite os_arch being aarch64`() {
        assertEquals("arm64-windows", parseBinaryenPlatformOrNull("Windows 11", "aarch64")?.platform)
    }

    @Test
    fun `macos on x86_64`() {
        assertEquals("x86_64-macos", parseBinaryenPlatformOrNull("Mac OS X", "x86_64")?.platform)
    }

    @Test
    fun `macos on arm uses arm64`() {
        assertEquals("arm64-macos", parseBinaryenPlatformOrNull("Mac OS X", "aarch64")?.platform)
    }

    @Test
    fun `linux on x86_64`() {
        assertEquals("x86_64-linux", parseBinaryenPlatformOrNull("Linux", "amd64")?.platform)
    }

    @Test
    fun `linux on arm uses aarch64`() {
        assertEquals("aarch64-linux", parseBinaryenPlatformOrNull("Linux", "aarch64")?.platform)
    }

    @Test
    fun `plain arm is resolved by os name`() {
        assertEquals("aarch64-linux", parseBinaryenPlatformOrNull("Linux", "arm")?.platform)
        assertEquals("arm64-macos", parseBinaryenPlatformOrNull("Mac OS X", "arm")?.platform)
        assertEquals("arm64-windows", parseBinaryenPlatformOrNull("Windows 11", "arm")?.platform)
    }

    @Test
    fun `freebsd is treated as linux`() {
        assertEquals("x86_64-linux", parseBinaryenPlatformOrNull("FreeBSD", "amd64")?.platform)
    }

    @Test
    fun `32-bit x86 is not supported`() {
        assertNull(parseBinaryenPlatformOrNull("Linux", "x86"))
    }

    @Test
    fun `unknown os is not supported`() {
        assertNull(parseBinaryenPlatformOrNull("Solaris", "amd64"))
    }

    @Test
    fun `all supported platforms can be parsed`() {
        val parsedPlatforms = listOf(
            parseBinaryenPlatformOrNull("Windows 11", "amd64"),
            parseBinaryenPlatformOrNull("Windows 11", "aarch64"),
            parseBinaryenPlatformOrNull("Mac OS X", "x86_64"),
            parseBinaryenPlatformOrNull("Mac OS X", "aarch64"),
            parseBinaryenPlatformOrNull("Linux", "amd64"),
            parseBinaryenPlatformOrNull("Linux", "aarch64"),
        ).mapNotNull { it?.platform }.toSet()

        assertEquals(BinaryenPlatform.SUPPORTED_PLATFORMS.toSet(), parsedPlatforms)
    }

    @Test
    fun `windows is detected`() {
        assertEquals(true, parseBinaryenPlatformOrNull("Windows 11", "amd64")?.isWindows())
        assertEquals(false, parseBinaryenPlatformOrNull("Linux", "amd64")?.isWindows())
    }
}
