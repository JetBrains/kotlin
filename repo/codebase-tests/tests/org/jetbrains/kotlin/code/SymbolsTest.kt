/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import com.ibm.icu.text.SpoofChecker
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.code.tools.FileMatcher
import org.jetbrains.kotlin.repoTestFixtures.isGitIgnored
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.code
import kotlin.test.fail

class SymbolsTest {
    private companion object {
        const val MAX_ASCII_CODE_POINT = 0x7F
    }

    @Test
    fun testNoNonAsciiInTrackedFileNames() {
        val allowlist = FileMatcher(File("."), emptyList())
        val failures = mutableListOf<String>()
        val allowedHits = mutableListOf<File>()

        for (file in gitTrackedFiles()) {
            val relative = FileUtil.toSystemIndependentName(file.path)
            val offending = relative.firstOrNull { it.code > MAX_ASCII_CODE_POINT } ?: continue

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

    @Test
    fun testNoAsciiConfusableLettersInTrackedFiles() {
        val filesWhereConfusableLettersAllowed = FileMatcher(
            File("."),
            listOf(
                "ChangeLog.md",
                "compiler/psi/psi-api/tests/org/jetbrains/kotlin/psi/PsiApiTest.kt",
                "compiler/testData/codegen/box/function/referenceBigArity.kt",
                "compiler/testData/codegen/boxWasmJsInterop/wasmJsStringBuiltins/jsOperationsJsString.kt",
                "compiler/testData/codegen/bytecodeText/accessorNaming.kt",
                "compiler/testData/diagnostics/tests/syntheticExtensions/javaProperties/OnlyAscii.kt",
                "compiler/testData/diagnostics/testsWithJsStdLib/name/nonASCIIName.kt",
                "compiler/tests-spec/testData/psi/linked/expressions/constant-literals/integer-literals/binary-integer-literals/p-1/neg/1.2.kt",
                "compiler/tests-spec/testData/psi/linked/expressions/constant-literals/integer-literals/binary-integer-literals/p-1/neg/1.2.txt",
                "compiler/tests-spec/testData/psi/linked/expressions/constant-literals/integer-literals/hexadecimal-integer-literals/p-1/neg/1.2.kt",
                "compiler/tests-spec/testData/psi/linked/expressions/constant-literals/integer-literals/hexadecimal-integer-literals/p-1/neg/1.2.txt",
                "docs/changelogs",
                "js/js.parser/src/org/jetbrains/kotlin/js/parser/sourcemaps/ECMA426BasedSourceMapParser.kt",
                "kotlin-native/backend.native/tests/samples/win32/src/win32Main/kotlin/MessageBox.kt",
                "kotlin-native/performance/ring/src/commonMain/kotlin/org/jetbrains/ring/zdf-win.kt",
                "libraries/stdlib/jvm/test/text/StringJVMTest.kt",
                "libraries/stdlib/native-wasm/test/harmony_regex/PatternTest2.kt",
                "libraries/stdlib/samples/test/samples/text/StringsJvmSpecific.kt",
                "libraries/stdlib/test/text/CharTest.kt",
                "libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/regressionTests/ConfigurationsTest.kt",
                "libraries/tools/kotlin-gradle-plugin/src/functionalTest/kotlin/org/jetbrains/kotlin/gradle/util/GradleLoggerInterceptor.kt",
                "native/native.tests/testData/CInterop/KT-55578/userSetupFancyHint.def",
                "native/native.tests/tests/org/jetbrains/kotlin/konan/test/blackbox/FrameworkTest.kt",
                "native/native.tests/tests/org/jetbrains/kotlin/konan/test/blackbox/LinkerOutputTestKT55578.kt",
            )
        )

        val binaryExtensions = setOf("jar", "class", "pdf", "png", "gz", "zip", "klib", "dll")

        val spoofChecker = SpoofChecker.Builder()
            .setChecks(SpoofChecker.CONFUSABLE)
            .build()

        val asciiLetterSkeletons = spoofChecker.getSkeleton(
            (('A'..'Z').toList() + ('a'..'z').toList()).joinToString("")
        )

        val asciiConfusabilityByCodePointCache = mutableMapOf<Int, Boolean>()

        @Suppress("RedundantIf")
        fun isAsciiConfusableLetter(codePoint: Int?): Boolean {
            if (codePoint == null || codePoint <= MAX_ASCII_CODE_POINT || !Character.isLetter(codePoint)) return false

            return asciiConfusabilityByCodePointCache.getOrPut(codePoint) {
                spoofChecker.getSkeleton(String(Character.toChars(codePoint))) in asciiLetterSkeletons
            }
        }

        data class AsciiConfusableLetterHit(val codePoint: Int, val indexInText: Int)

        fun firstAsciiConfusableLetter(text: String): AsciiConfusableLetterHit? {
            @Suppress("SpellCheckingInspection")
            fun Int?.isNonAskiiLetter() =
                this != null && this > MAX_ASCII_CODE_POINT && Character.isLetter(this)

            for (index in text.indices) {
                val codePoint = text.codePointAt(index)
                if (isAsciiConfusableLetter(codePoint)) {
                    val before = if (index > 0) text.codePointAt(index - 1) else null
                    val after = if (index < text.lastIndex) text.codePointAt(index + 1) else null

                    if (before.isNonAskiiLetter() || after.isNonAskiiLetter()) {
                        // Reduce number of false positives when there's several non-ASKII letters stay in a row
                        // But at least one of them should be non-confusable
                        if (!isAsciiConfusableLetter(before) || !isAsciiConfusableLetter(after)) {
                            continue
                        }
                    }

                    return AsciiConfusableLetterHit(codePoint, index)
                }
            }
            return null
        }

        fun describeAsciiConfusableLetterHit(text: String, hit: AsciiConfusableLetterHit): String {
            val (codePoint, indexInText) = hit

            val lineStart = text.lastIndexOf('\n', indexInText) + 1
            val lineEnd = text.indexOf('\n', lineStart).takeIf { it >= 0 } ?: text.length

            val lineNumber = 1 + text.take(lineStart).count { it == '\n' }

            val range = 20
            val before = text.substring(maxOf(lineStart, indexInText - range), indexInText)
            val after = text.substring(indexInText + 1, minOf(indexInText + 1 + range, lineEnd))
            val char = text[indexInText]
            val excerpt = "$before>>$char<<$after"

            return "$lineNumber: ${describeCodePoint(codePoint)}: $excerpt"
        }

        val failures = mutableListOf<String>()
        val allowedHits = mutableListOf<File>()

        for (file in gitTrackedFiles()) {
            if (!file.isFile) continue
            if (file.extension in binaryExtensions) continue

            val bytes = file.readBytes()
            val text = bytes.toString(Charsets.UTF_8)
            val offending = firstAsciiConfusableLetter(text) ?: continue

            if (bytes.contains(0.toByte())) {
                // Additional heuristic for binary files. Few files remain after the extension check, so scan the content
                // a second time only when it also contains an offending character.
                continue
            }

            if (filesWhereConfusableLettersAllowed.matchWithContains(file)) {
                allowedHits.add(file)
            } else {
                val relative = FileUtil.toSystemIndependentName(file.path)
                failures.add("$relative:${describeAsciiConfusableLetterHit(text, offending)}")
            }
        }

        val unusedFiles = filesWhereConfusableLettersAllowed.unmatched(allowedHits)
        if (failures.isEmpty() && unusedFiles.isEmpty()) return

        fail(
            buildString {
                if (failures.isNotEmpty()) {
                    appendLine("Non-ASCII letters confusable with ASCII letters:")
                    failures.forEach { appendLine(it) }
                }
                if (unusedFiles.isNotEmpty()) {
                    appendLine("Unused allowlist entries:")
                    unusedFiles.forEach { appendLine(it) }
                }
            }
        )
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
