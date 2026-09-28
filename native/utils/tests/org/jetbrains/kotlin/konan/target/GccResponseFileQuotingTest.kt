/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.TempFiles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readLines

class GccResponseFileQuotingTest {
    private val cases = listOf(
        // Windows path: every backslash must survive (KT-89637).
        """D:\a\b c\lib.a""" to """"D:\\a\\b c\\lib.a"""",
        // Unix path with a backslash.
        """/tmp/a\b/lib.a""" to """"/tmp/a\\b/lib.a"""",
        // Unix path with a double quote.
        """/tmp/a"b/lib.a""" to """"/tmp/a\"b/lib.a"""",
        // Unix path with a backslash directly before a double quote.
        """/tmp/a\"b/lib.a""" to """"/tmp/a\\\"b/lib.a"""",
        // Spaces and single quotes need only the surrounding double quotes.
        """/tmp/it's here/lib.a""" to """"/tmp/it's here/lib.a"""",
        // Plain path.
        "/usr/lib/libx.a" to "\"/usr/lib/libx.a\"",
    )

    @Test
    fun testQuoting() {
        for ((path, expected) in cases) {
            assertEquals(expected, gnuResponseFileQuoted(path), "Incorrect quoting of $path")
        }
    }

    @Test
    fun testResponseFileContents() {
        val tempFiles = TempFiles()
        try {
            val paths = cases.map { it.first }
            val argument = paths.asGccSpreadArgument("static", tempFiles).single()
            assertTrue(argument.startsWith("@"))
            val responseFile = Path(argument.removePrefix("@"))
            assertEquals(cases.map { it.second }, responseFile.readLines())
        } finally {
            tempFiles.dispose()
        }
    }

    @Test
    fun testNoResponseFileForEmptyList() {
        val tempFiles = TempFiles()
        try {
            assertEquals(emptyList<String>(), emptyList<String>().asGccSpreadArgument("static", tempFiles))
        } finally {
            tempFiles.dispose()
        }
    }
}
