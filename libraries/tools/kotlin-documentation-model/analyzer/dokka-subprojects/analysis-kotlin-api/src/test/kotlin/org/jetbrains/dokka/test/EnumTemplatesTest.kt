/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.test

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.assertAll
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.asserter

/**
 * Documentation for Enum's synthetic `values()` and `valueOf()` functions is only present in source code,
 * but not present in the descriptors. However, Dokka needs to generate documentation for these functions,
 * so it ships with hardcoded kdoc templates.
 *
 * This test exists to make sure the kdoc from Kotlin stdlib for the hardcoded synthetic enum functions
 * matches (sometimes approximately) Dokka's templates.
 */
class EnumTemplatesTest {

    @Test
    fun enumValueOf() {
        listOf(
            "Returns the enum constant of this type with the specified name.",
            "(Extraneous whitespace characters are not permitted.)",
        ).forEach { line ->
            assertSubstringIsInFiles(line, actualStdlibEnumKt)
            assertContains(enumValueOfTemplate, line)
        }

        "The string must match exactly an identifier used to declare an enum constant in this type.".let { line ->
            assertSubstringIsInFiles(line, actualStdlibEnumKt)
            assertContains(
                enumValueOfTemplate,
                // The Dokka template has a newline, but otherwise the text is the same.
                line.replace("identifier used to declare", "identifier used\nto declare"),
            )
        }

        "@throws IllegalArgumentException if this enum type has no constant with the specified name".let { line ->
            assertSubstringIsInFiles(line, actualStdlibEnumKt)
            assertContains(
                enumValueOfTemplate,
                // The Dokka template uses the FQN of IllegalArgumentException
                line.replace("IllegalArgumentException", "kotlin.IllegalArgumentException"),
            )
        }
    }

    @Test
    fun enumValues() {
        listOf(
            "Returns an array containing the constants of this enum type, in the order they're declared.",
            "This method may be used to iterate over the constants.",
        ).forEach { line ->
            assertSubstringIsInFiles(line, actualStdlibEnumKt)
            assertContains(enumValuesTemplate, line)

        }
    }

    @Test
    fun enumEntries() {
        listOf(
            "Returns an immutable [kotlin.enums.EnumEntries] list containing the constants of this enum type, in the order they're declared."
        ).forEach { line ->
            assertSubstringIsInFiles(line, actualStdlibEnumKt)
            assertContains(enumEntriesTemplate, line)

        }
    }

    companion object {

        /**
         * Assert that all [files] exist, are files, and contain [substring].
         */
        private fun assertSubstringIsInFiles(substring: String, vararg files: Path) {
            assertAll(files.map { file ->
                {
                    assertTrue(Files.exists(file), "File does not exist: $file")
                    assertTrue(Files.isRegularFile(file), "File is not a regular file: $file")
                    assertContains(file.toFile().readText(), substring)
                }
            })
        }

        private fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

        // TODO replace with kotlin.test.assertContains after migrating to Kotlin 1.5+
        // https://github.com/JetBrains/kotlin/blob/c072e7c945fed74805d87ecc89c9a650bad23e12/libraries/kotlin.test/common/src/main/kotlin/kotlin/test/Assertions.kt#L334-L345
        internal fun assertContains(
            charSequence: CharSequence,
            other: CharSequence,
            ignoreCase: Boolean = false,
            message: String? = null,
        ) {
            asserter.assertTrue(
                { messagePrefix(message) + "Expected the char sequence to contain the substring.\nCharSequence <$charSequence>, substring <$other>, ignoreCase <$ignoreCase>." },
                charSequence.contains(other, ignoreCase)
            )
        }

        private fun loadResource(@Language("file-reference") path: String): String {
            return EnumTemplatesTest::class.java.getResource(path)
                ?.readText()
                ?: error("Failed to load resource: $path")
        }

        private val enumEntriesTemplate: String = loadResource("/dokka/docs/kdoc/EnumEntries.kt.template")
        private val enumValueOfTemplate: String = loadResource("/dokka/docs/kdoc/EnumValueOf.kt.template")
        private val enumValuesTemplate: String = loadResource("/dokka/docs/kdoc/EnumValues.kt.template")

        /**
         * Base directory for the unpacked Kotlin stdlib source code.
         * The system property must be set in the Gradle task.
         */
        private val kotlinStdlibSourcesDir: Path by lazy {
            val sourcesDir = System.getProperty("kotlinStdlibSourcesDir")
                ?: error("Missing 'kotlinStdlibSourcesDir' system property")
            Paths.get(sourcesDir)
        }

        /** Get the actual `Enum.kt` source file from Kotlin stdlib. */
        private val actualStdlibEnumKt: Path by lazy {
            kotlinStdlibSourcesDir.resolve("jvmMain/kotlin/Enum.kt")
        }
    }
}