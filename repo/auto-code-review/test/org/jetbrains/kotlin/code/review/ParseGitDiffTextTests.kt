/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The diff fragments are verbatim `git diff --default-prefix <base>` output (git 2.51.0) from a scratch repository.
 *
 * Note: Git appends a tab to the `---` and `+++` lines when the path contains a space.
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

    private val modifiedFileWithSpaces = """
        diff --git a/a b/c d.txt b/a b/c d.txt
        index 367efad..d645ca2 100644
        --- a/a b/c d.txt${'\t'}
        +++ b/a b/c d.txt${'\t'}
        @@ -1,5 +1,5 @@
         c d.txt line 1
         c d.txt line 2
        -c d.txt line 3
        +c d.txt line 3 changed
         c d.txt line 4
         c d.txt line 5
    """.trimIndent()

    @Test
    fun `modified file with spaces`() = assertChangedFile(
        modifiedFileWithSpaces,
        oldPath = "a b/c d.txt",
        newPath = "a b/c d.txt",
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

    private val addedEmptyFileWithSpaces = """
        diff --git a/a b/empty file.txt b/a b/empty file.txt
        new file mode 100644
        index 0000000..e69de29
    """.trimIndent()

    @Test
    fun `added empty file with spaces`() = assertChangedFile(
        addedEmptyFileWithSpaces,
        oldPath = null,
        newPath = "a b/empty file.txt",
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

    private val deletedFileWithSpaces = """
        diff --git a/a b/deleted file.txt b/a b/deleted file.txt
        deleted file mode 100644
        index f29884e..0000000
        --- a/a b/deleted file.txt${'\t'}
        +++ /dev/null
        @@ -1,3 +0,0 @@
        -deleted file.txt line 1
        -deleted file.txt line 2
        -deleted file.txt line 3
    """.trimIndent()

    @Test
    fun `deleted file with spaces`() = assertChangedFile(
        deletedFileWithSpaces,
        oldPath = "a b/deleted file.txt",
        newPath = null,
    )

    private val renamedFile = """
        diff --git a/old/Renamed.kt b/new/Renamed.kt
        similarity index 100%
        rename from old/Renamed.kt
        rename to new/Renamed.kt
    """.trimIndent()

    @Test
    fun `renamed file`() = assertChangedFile(
        renamedFile,
        oldPath = "old/Renamed.kt",
        newPath = "new/Renamed.kt",
    )

    private val renamedFileWithSpaces = """
        diff --git a/a b/old name.txt b/a b/new name.txt
        similarity index 100%
        rename from a b/old name.txt
        rename to a b/new name.txt
    """.trimIndent()

    @Test
    fun `renamed file with spaces`() = assertChangedFile(
        renamedFileWithSpaces,
        oldPath = "a b/old name.txt",
        newPath = "a b/new name.txt",
    )

    private val renamedAndModifiedFileWithSpaces = """
        diff --git a/a b/before.txt b/c d/after.txt
        similarity index 86%
        rename from a b/before.txt
        rename to c d/after.txt
        index a644eaa..a60d36d 100644
        --- a/a b/before.txt${'\t'}
        +++ b/c d/after.txt${'\t'}
        @@ -2,7 +2,7 @@ before.txt line 1
         before.txt line 2
         before.txt line 3
         before.txt line 4
        -before.txt line 5
        +before.txt line 5 changed
         before.txt line 6
         before.txt line 7
         before.txt line 8
    """.trimIndent()

    @Test
    fun `renamed and modified file with spaces`() = assertChangedFile(
        renamedAndModifiedFileWithSpaces,
        oldPath = "a b/before.txt",
        newPath = "c d/after.txt",
    )

    private val modeChangedFile = """
        diff --git a/script.sh b/script.sh
        old mode 100644
        new mode 100755
    """.trimIndent()

    @Test
    fun `mode changed file`() = assertChangedFile(
        modeChangedFile,
        oldPath = "script.sh",
        newPath = "script.sh",
    )

    private val modeAndContentChangedFileWithSpaces = """
        diff --git a/my script.sh b/my script.sh
        old mode 100644
        new mode 100755
        index 75c59fe..1920964
        --- a/my script.sh${'\t'}
        +++ b/my script.sh${'\t'}
        @@ -1,3 +1,4 @@
         my script.sh line 1
         my script.sh line 2
         my script.sh line 3
        +my script.sh line 4
    """.trimIndent()

    @Test
    fun `mode and content changed file with spaces`() = assertChangedFile(
        modeAndContentChangedFileWithSpaces,
        oldPath = "my script.sh",
        newPath = "my script.sh",
    )

    private val renamedAndModeChangedFile = """
        diff --git a/tool.sh b/bin/tool.sh
        old mode 100644
        new mode 100755
        similarity index 100%
        rename from tool.sh
        rename to bin/tool.sh
    """.trimIndent()

    @Test
    fun `renamed and mode changed file`() = assertChangedFile(
        renamedAndModeChangedFile,
        oldPath = "tool.sh",
        newPath = "bin/tool.sh",
    )

    private val modifiedBinaryFile = """
        diff --git a/image.bin b/image.bin
        index 61f018b..2205ed4 100644
        Binary files a/image.bin and b/image.bin differ
    """.trimIndent()

    @Test
    fun `modified binary file`() = assertChangedFile(
        modifiedBinaryFile,
        oldPath = "image.bin",
        newPath = "image.bin",
    )

    private val addedBinaryFileWithSpaces = """
        diff --git a/a b/new image.bin b/a b/new image.bin
        new file mode 100644
        index 0000000..806f43c
        Binary files /dev/null and b/a b/new image.bin differ
    """.trimIndent()

    @Test
    fun `added binary file with spaces`() = assertChangedFile(
        addedBinaryFileWithSpaces,
        oldPath = null,
        newPath = "a b/new image.bin",
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

    private val modifiedFileWithQuotedPath = """
        diff --git "a/a b/caf\303\251.txt" "b/a b/caf\303\251.txt"
        index 6537b0e..8b77123 100644
        --- "a/a b/caf\303\251.txt"${'\t'}
        +++ "b/a b/caf\303\251.txt"${'\t'}
        @@ -1,5 +1,5 @@
         café.txt line 1
         café.txt line 2
        -café.txt line 3
        +café.txt line 3 changed
         café.txt line 4
         café.txt line 5
    """.trimIndent()

    @Test
    fun `modified file with quoted path`() = assertChangedFile(
        modifiedFileWithQuotedPath,
        oldPath = "a b/café.txt",
        newPath = "a b/café.txt",
    )

    private val renamedFileToQuotedPath = """
        diff --git a/plain.txt "b/say \"hi\" \303\274.txt"
        similarity index 100%
        rename from plain.txt
        rename to "say \"hi\" \303\274.txt"
    """.trimIndent()

    @Test
    fun `renamed file to quoted path`() = assertChangedFile(
        renamedFileToQuotedPath,
        oldPath = "plain.txt",
        newPath = "say \"hi\" ü.txt",
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

    // Generated with `-c core.quotePath=false`, so that U+2028 and U+0085 are not escaped.
    private val modifiedFileWithLineTerminatorsInQuotedPath = """
        diff --git "a/quoted \"ls${'\u2028'}nel${'\u0085'}\".txt" "b/quoted \"ls${'\u2028'}nel${'\u0085'}\".txt"
        index a92d664..48721fb 100644
        --- "a/quoted \"ls${'\u2028'}nel${'\u0085'}\".txt"${'\t'}
        +++ "b/quoted \"ls${'\u2028'}nel${'\u0085'}\".txt"${'\t'}
        @@ -1,3 +1,3 @@
         line 1
        -line 2
        +line 2 changed
         line 3
    """.trimIndent()

    @Test
    fun `modified file with U+2028 and U+0085 in quoted path`() = assertChangedFile(
        modifiedFileWithLineTerminatorsInQuotedPath,
        oldPath = "quoted \"ls\u2028nel\u0085\".txt",
        newPath = "quoted \"ls\u2028nel\u0085\".txt",
    )

    // Generated with `-C -C`, which is what `diff.renames=copies` does.
    private val copiedFileWithSpaces = """
        diff --git a/source.txt b/a b/copy of source.txt
        similarity index 100%
        copy from source.txt
        copy to a b/copy of source.txt
    """.trimIndent()

    @Test
    fun `copied file with spaces`() = assertChangedFile(
        copiedFileWithSpaces,
        oldPath = "source.txt",
        newPath = "a b/copy of source.txt",
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
            modifiedFileWithSpaces,
            modifiedFileWithCrlf,
            addedFile,
            addedEmptyFileWithSpaces,
            deletedFile,
            deletedFileWithSpaces,
            renamedFile,
            renamedFileWithSpaces,
            renamedAndModifiedFileWithSpaces,
            modeChangedFile,
            modeAndContentChangedFileWithSpaces,
            renamedAndModeChangedFile,
            modifiedBinaryFile,
            addedBinaryFileWithSpaces,
            addedSymlink,
            modifiedFileWithQuotedPath,
            renamedFileToQuotedPath,
            modifiedFileWithLineTerminatorsInPath,
            modifiedFileWithLineTerminatorsInQuotedPath,
            copiedFileWithSpaces,
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
