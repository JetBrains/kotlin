/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework.inputchecking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TestInputsCheckerTest {

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        // on macOS, the temp dir path contains a symlink "/var" -> "/private/var"
        tempDir = tempDir.toFile().canonicalFile.toPath()
    }

    @Test
    fun `declared input is allowed`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createDeclaredFile()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(emptySet<String>(), detected)
    }

    @Test
    fun `non-canonical declared input is allowed`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createNonCanonicalDeclaredFile()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(emptySet<String>(), detected)
    }

    @Test
    fun `undeclared input is detected`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createUndeclaredFile()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(fixture.undeclared, detected)
    }

    @Test
    fun `non-canonical undeclared input is detected`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createNonCanonicalUndeclaredFile()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        // The event carries the (absolute, but non-canonical) path that was actually accessed.
        assertEquals(fixture.undeclaredNonCanonical, detected)
    }

    @Test
    fun `null path is ignored`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            addNull()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(emptySet<String>(), detected)
    }

    @Test
    fun `file outside root dir is allowed`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createFileOutsideRootDir()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(emptySet<String>(), detected)
    }

    @Test
    fun `file inside build dir is allowed`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createFileInsideBuildDir()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(emptySet<String>(), detected)
    }

    @Test
    fun `dynamically created klib cache file is allowed`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createKlibCacheFile()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(emptySet<String>(), detected)
    }

    @Test
    fun `klib stdlib cache file is detected`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createKlibStdlibCacheFile()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(fixture.klibStdlibCacheFiles, detected)
    }

    @Test
    fun `directory is ignored`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createDirectory()
        }

        // when
        val detected = fixture.checkAllPaths()

        // then
        assertEquals(emptySet<String>(), detected)
    }

    @Test
    fun `already detected undeclared input is not reported twice`() {
        // given
        val fixture = TestInputsFixture(tempDir) {
            createUndeclaredFile()
        }

        // when
        val firstDetection = fixture.checkAllPaths()

        // and then
        val secondDetection = fixture.checkAllPaths()

        // then
        assertEquals(fixture.undeclared, firstDetection)
        assertEquals(emptySet<String>(), secondDetection)
    }

    @Test
    fun `failFast throws on undeclared input`() {
        // given
        val fixture = TestInputsFixture(tempDir, failFast = true) {
            createUndeclaredFile()
        }

        // when
        val exception = assertThrows<UndeclaredInputException> {
            fixture.checkAllPaths()
        }

        // then
        assertTrue(fixture.undeclared.first() in exception.message.orEmpty())
    }

    @Test
    fun `failFast allows declared input`() {
        // given
        val fixture = TestInputsFixture(tempDir, failFast = true) {
            createDeclaredFile()
        }

        // expect not to throw
        fixture.checkAllPaths()
    }
}
