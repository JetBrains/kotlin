/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.code.tools.FileMatcher
import org.jetbrains.kotlin.repoTestFixtures.isGitIgnored
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.code
import kotlin.test.fail

class SymbolsTest {
    @Test
    fun testNoNonAsciiInTrackedFileNames() {
        val allowlist = FileMatcher(File("."), emptyList())
        val failures = mutableListOf<String>()
        val allowedHits = mutableListOf<File>()

        for (file in gitTrackedFiles()) {
            val relative = FileUtil.toSystemIndependentName(file.path)
            val offending = relative.firstOrNull { it.code > 0x7F } ?: continue

            if (allowlist.matchWithContains(file)) {
                allowedHits.add(file)
            } else {
                failures.add("$relative: ${describeCodePoint(offending.code)}")
            }
        }

        val unused = allowlist.unmatched(allowedHits)
        if (failures.isNotEmpty()) {
            fail((listOf("Non-ASCII characters in tracked file/directory names:") + failures).joinToString("\n"))
        }
        if (unused.isNotEmpty()) {
            fail((listOf("Unused allowlist entries:") + unused).joinToString("\n"))
        }
    }

    private fun gitTrackedFiles(): Sequence<File> {
        val root = File(".")
        return root.walkTopDown()
            .onEnter { dir -> dir == root || !dir.toPath().isGitIgnored() }
            .filter { it != root && !it.toPath().isGitIgnored() }
    }

    private fun describeCodePoint(codePoint: Int): String {
        val name = Character.getName(codePoint) ?: "UNKNOWN"
        return "'${String(Character.toChars(codePoint))}' (U+${codePoint.toString(16).uppercase().padStart(4, '0')} $name)"
    }
}
