/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertAll
import utils.assertContains
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

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
            assertSubstringIsInFiles(line, enumValueOfTemplate, actualStdlibEnumKt)
        }

        "The string must match exactly an identifier used to declare an enum constant in this type.".let { line ->
            assertSubstringIsInFiles(line, actualStdlibEnumKt)
            assertSubstringIsInFiles(
                // The Dokka template has a newline, but otherwise the text is the same.
                line.replace("identifier used to declare", "identifier used\nto declare"),
                enumValueOfTemplate,
            )
        }

        "@throws IllegalArgumentException if this enum type has no constant with the specified name".let { line ->
            assertSubstringIsInFiles(line, actualStdlibEnumKt)
            assertSubstringIsInFiles(
                // The Dokka template uses the FQN of IllegalArgumentException
                line.replace("IllegalArgumentException", "kotlin.IllegalArgumentException"),
                enumValueOfTemplate,
            )
        }
    }

    @Test
    fun enumValues() {
        listOf(
            "Returns an array containing the constants of this enum type, in the order they're declared.",
            "This method may be used to iterate over the constants.",
        ).forEach { line ->
            assertSubstringIsInFiles(line, enumValuesTemplate, actualStdlibEnumKt)
        }
    }

    /**
     * This test is disabled because the `Enum.entries` does not have accessible documentation.
     *
     * See https://youtrack.jetbrains.com/issue/KTIJ-23569/Provide-quick-documentation-for-Enum.entries
     */
    @Test
    @Disabled("Kotlin stdlib does not have kdoc for Enum.entries")
    fun enumEntries() {
        listOf(
            "Returns a representation of an immutable list of all enum entries, in the order they're declared.",
            "This method may be used to iterate over the enum entries.",
        ).forEach { line ->
            assertSubstringIsInFiles(line, enumEntriesTemplate, actualStdlibEnumKt)
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

        private fun loadResource(@Language("file-reference") path: String): Path {
            val resource = EnumTemplatesTest::class.java.getResource(path)
                ?.toURI()
                ?: error("Failed to load resource: $path")
            return Paths.get(resource)
        }

        private val enumEntriesTemplate: Path = loadResource("/dokka/docs/kdoc/EnumEntries.kt.template")
        private val enumValueOfTemplate: Path = loadResource("/dokka/docs/kdoc/EnumValueOf.kt.template")
        private val enumValuesTemplate: Path = loadResource("/dokka/docs/kdoc/EnumValues.kt.template")

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
