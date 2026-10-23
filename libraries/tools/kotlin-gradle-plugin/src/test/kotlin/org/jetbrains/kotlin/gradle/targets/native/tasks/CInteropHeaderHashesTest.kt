/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

class CInteropHeaderHashesTest {

    @Test
    fun `hashes round-trip through the file`(@TempDir tempDir: File) {
        // the hash is a raw MD5 digest decoded as text, so it holds control and replacement characters
        val hashes = mapOf(
            "/include/a.h" to "\u0000\u001f�\"\\",
            "/include/b.h" to "plain",
        )
        val file = tempDir.resolve("cache/headers.json")

        file.writeHeaderHashes(hashes)

        assertEquals(hashes, file.readHeaderHashes())
    }

    @Test
    fun `the written file is pretty-printed`(@TempDir tempDir: File) {
        val file = tempDir.resolve("headers.json")

        file.writeHeaderHashes(mapOf("/include/a.h" to "one", "/include/b.h" to "two"))

        assertEquals(
            """
            {
                "/include/a.h": "one",
                "/include/b.h": "two"
            }
            """.trimIndent(),
            file.readText(),
        )
    }
}
