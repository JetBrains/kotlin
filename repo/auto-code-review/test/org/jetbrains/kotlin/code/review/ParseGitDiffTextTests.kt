/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The diff fragments are verbatim `git diff --default-prefix <base>` output (git 2.51.0) from a scratch repository.
 */
class ParseGitDiffTextTests {
    private val modifiedFile = """
        diff --git a/src/Modified.kt b/src/Modified.kt
        index df7a222..5f39417 100644
        --- a/src/Modified.kt
        +++ b/src/Modified.kt
        @@ -1,5 +1,5 @@
         Modified.kt line 1
         Modified.kt line 2
        -Modified.kt line 3
        +Modified.kt line 3 changed
         Modified.kt line 4
         Modified.kt line 5
    """.trimIndent()

    @Test
    fun `modified file`() = assertChangedFile(
        modifiedFile,
        oldPath = "src/Modified.kt",
        newPath = "src/Modified.kt",
    )

    // Not a raw string, because `trimIndent()` would drop the `\r` characters.
    private val modifiedFileWithCrlf = listOf(
        "diff --git a/crlf.txt b/crlf.txt",
        "index bce6b08..d2eac8e 100644",
        "--- a/crlf.txt",
        "+++ b/crlf.txt",
        "@@ -1,3 +1,3 @@",
        " crlf line 1\r",
        "-crlf line 2\r",
        "+crlf line 2 changed\r",
        " crlf line 3\r",
    ).joinToString("\n")

    @Test
    fun `modified file with CRLF line endings`() = assertChangedFile(
        modifiedFileWithCrlf,
        oldPath = "crlf.txt",
        newPath = "crlf.txt",
    )

    private val addedFile = """
        diff --git a/added.txt b/added.txt
        new file mode 100644
        index 0000000..7cf2cf2
        --- /dev/null
        +++ b/added.txt
        @@ -0,0 +1,3 @@
        +added.txt line 1
        +added.txt line 2
        +added.txt line 3
    """.trimIndent()

    @Test
    fun `added file`() = assertChangedFile(
        addedFile,
        oldPath = null,
        newPath = "added.txt",
    )

    private val deletedFile = """
        diff --git a/deleted.txt b/deleted.txt
        deleted file mode 100644
        index 85b4cb1..0000000
        --- a/deleted.txt
        +++ /dev/null
        @@ -1,3 +0,0 @@
        -deleted.txt line 1
        -deleted.txt line 2
        -deleted.txt line 3
    """.trimIndent()

    @Test
    fun `deleted file`() = assertChangedFile(
        deletedFile,
        oldPath = "deleted.txt",
        newPath = null,
    )

    private val addedSymlink = """
        diff --git a/link b/link
        new file mode 120000
        index 0000000..1de5659
        --- /dev/null
        +++ b/link
        @@ -0,0 +1 @@
        +target
        \ No newline at end of file
    """.trimIndent()

    @Test
    fun `added symlink`() = assertChangedFile(
        addedSymlink,
        oldPath = null,
        newPath = "link",
    )

    // U+2028 and U+0085 are line terminators for the `.` in Java regular expressions.
    // Generated with `-c core.quotePath=false`, so that these characters are not escaped.
    private val modifiedFileWithLineTerminatorsInPath = """
        diff --git a/ls${'\u2028'}nel${'\u0085'}.txt b/ls${'\u2028'}nel${'\u0085'}.txt
        index a92d664..48721fb 100644
        --- a/ls${'\u2028'}nel${'\u0085'}.txt
        +++ b/ls${'\u2028'}nel${'\u0085'}.txt
        @@ -1,3 +1,3 @@
         line 1
        -line 2
        +line 2 changed
         line 3
    """.trimIndent()

    @Test
    fun `modified file with U+2028 and U+0085 in path`() = assertChangedFile(
        modifiedFileWithLineTerminatorsInPath,
        oldPath = "ls\u2028nel\u0085.txt",
        newPath = "ls\u2028nel\u0085.txt",
    )

    private fun assertChangedFile(diffFragment: String, oldPath: String?, newPath: String?) {
        val lines = diffFragment.split('\n')
        val changedFile = parseChangedFile(lines)
        assertEquals(oldPath, changedFile.oldPath?.pathFromProjectRoot)
        assertEquals(newPath, changedFile.newPath?.pathFromProjectRoot)
        assertEquals(lines, changedFile.patchLines)
    }

    @Test
    fun `diff is split into changed files`() {
        val diffFragments = listOf(
            modifiedFile,
            modifiedFileWithCrlf,
            addedFile,
            deletedFile,
            addedSymlink,
            modifiedFileWithLineTerminatorsInPath,
        )

        // Git terminates each line with a line break, including the last one.
        val diffText = diffFragments.joinToString(separator = "") { "$it\n" }

        assertEquals(
            diffFragments.map { it.split('\n') },
            parseGitDiffText(diffText).map { it.patchLines },
        )
    }

    @Test
    fun `empty diff has no changed files`() {
        assertEquals(emptyList(), parseGitDiffText(""))
    }
}
