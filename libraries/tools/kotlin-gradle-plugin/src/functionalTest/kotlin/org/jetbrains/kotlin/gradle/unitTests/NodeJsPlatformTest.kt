/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.targets.js.nodejs.OsType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.Platform
import org.jetbrains.kotlin.gradle.targets.js.nodejs.parseOsType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.parsePlatform
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NodeJsPlatformTest {

    @ParameterizedTest
    @CsvSource(
        "Mac OS X,    x86_64,  darwin, x64",
        "Mac OS X,    aarch64, darwin, arm64",
        "Linux,       amd64,   linux,  x64",
        "Linux,       aarch64, linux,  arm64",
        "Linux,       ppc64le, linux,  ppc64le",
        "Linux,       s390x,   linux,  s390x",
        "Windows 11,  amd64,   win,    x64",
        "Windows 11,  aarch64, win,    arm64",
    )
    fun `os name and arch map to a published Node js distribution`(osName: String, osArch: String, name: String, arch: String) {
        assertEquals(Platform(name, arch), parsePlatform(parseOsType(osName), osArch))
    }

    @ParameterizedTest
    @CsvSource(
        "FreeBSD,     amd64,   linux,  x64",
        "Linux,       AARCH64, linux,  arm64",
        "Linux,       arm,     linux,  armv7l",
        "Linux,       aarch32, linux,  armv7l",
        "Linux,       arm64,   linux,  arm64",
        "Mac OS X,    arm64,   darwin, arm64",
        "Linux,       x86,     linux,  x86",
        "Linux,       i386,    linux,  x86",
        "Windows 10,  x86,     win,    x86",
        "Windows 10,  arm,     win,    arm64",
        "Windows 10,  AARCH64, win,    arm64",
    )
    fun `edge case architectures are resolved from os arch only`(osName: String, osArch: String, name: String, arch: String) {
        assertEquals(Platform(name, arch), parsePlatform(parseOsType(osName), osArch))
    }

    @ParameterizedTest
    @CsvSource(
        "Windows 11, WINDOWS",
        "Mac OS X,   MAC",
        "Linux,      LINUX",
        "FreeBSD,    FREEBSD",
    )
    fun `os name is parsed to os type`(osName: String, expected: String) {
        assertEquals(OsType.valueOf(expected), parseOsType(osName))
    }

    @ParameterizedTest
    @ValueSource(strings = ["SunOS", "AIX", "OS/400", ""])
    fun `unsupported os name fails`(osName: String) {
        val exception = assertFailsWith<IllegalStateException> { parseOsType(osName) }
        assertTrue(exception.message.orEmpty().startsWith("Unsupported OS"), "Unexpected message: ${exception.message}")
    }

    @Test
    fun `freebsd uses linux distribution name`() {
        assertEquals("linux", parsePlatform(OsType.FREEBSD, "amd64").name)
    }
}
