/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.nio.file.Files

private const val ENV_GENERATE_GOLDEN = "GENERATE_GOLDEN"
private const val GOLDEN_FILE_TYPE = "txt"
private fun env(name: String): Boolean = (System.getenv(name) ?: "false").toBoolean()
private fun envList(name: String): List<String> = (System.getenv(name) ?: "").quotedSplit()

/**
 * GoldenTransformRule
 *
 * Compare transformed IR source to a golden test file. Golden files contain both the
 * pre-transformed and post-transformed source for easier review.
 * To regenerate the set of golden tests, pass GENERATE_GOLDEN=true as an environment variable.
 *
 * @param pathToGoldens: Path to golden files
 * @param generateGoldens: When true, will generate the golden test file and replace any existing
 * @param generateGoldenFiles: Generate the golden file if the name (without extension, is in the
 *          list.
 * @param generateMissingGoldens: When true, will generate a golden file for any that are not found.
 **/
class GoldenTransformRule(
    private val pathToGoldens: String = "$TEST_RESOURCES_ROOT/golden",
    private val generateGoldens: Boolean = env(ENV_GENERATE_GOLDEN),
    private val generateGoldenFiles: Set<String> = envList(ENV_GENERATE_GOLDEN).toSet(),
    private val generateMissingGoldens: Boolean = true
) : TestRule {
    private lateinit var goldenFile: File
    private lateinit var testIdentifier: String

    private val testWatcher = object : TestWatcher() {
        override fun starting(description: Description) {
            val goldenFilePath = getGoldenFilePath(description.className, description.methodName)
            goldenFile = File(goldenFilePath)
            testIdentifier = "${description.className}_${description.methodName}"
        }
    }

    private fun getGoldenFilePath(
        className: String,
        methodName: String
    ) = "$pathToGoldens/$className/$methodName.$GOLDEN_FILE_TYPE"

    override fun apply(base: Statement, description: Description): Statement {
        return base.run {
            testWatcher.apply(this, description)
        }
    }

    /**
     * Verify the current test against the matching golden file.
     * If generateGoldens is true, the golden file will first be generated.
     */
    fun verifyGolden(testInfo: GoldenTransformTestInfo) {
        if (
            generateGoldens || (!goldenFile.exists() && generateMissingGoldens) ||
            goldenFile.nameWithoutExtension in generateGoldenFiles
        ) {
            saveGolden(testInfo)
        }

        if (!goldenFile.exists()) {
            throw FileNotFoundException("Could not find golden file: ${goldenFile.absolutePath}")
        }

        val loadedTestInfo = try {
            GoldenTransformTestInfo.fromEncodedString(goldenFile.readText())
        } catch (e: IllegalStateException) {
            error("Golden ${goldenFile.absolutePath} file could not be parsed.\n${e.message}")
        }

        // Use absolute path in the assert error so studio shows it as a link
        Assert.assertEquals(
            "Transformed source does not match golden file:" +
                "\n${goldenFile.absolutePath}\n" +
                "To regenerate golden files, set GENERATE_GOLDEN=\"${
                    goldenFile.nameWithoutExtension}\" as an env variable (or set it to 'true' " +
                "to generate all the files).\n" +
                "The environment variable can be a comma delimited list of names (the quotes are " +
                "optional)",
            loadedTestInfo.transformed,
            testInfo.transformed
        )
    }

    private fun saveGolden(testInfo: GoldenTransformTestInfo) {
        val directory = goldenFile.parentFile!!
        if (!directory.exists()) {
            Files.createDirectories(directory.toPath())
        }
        goldenFile.writeText(testInfo.encodeToString())
    }
}

/**
 * GoldenTransformTestInfo
 * @param source The pre-transformed source code.
 * @param transformed Post transformed IR tree source.
 */
data class GoldenTransformTestInfo(
    val source: String,
    val transformed: String
) {
    fun encodeToString(): String =
        buildString {
            append(SOURCE_HEADER)
            appendLine()
            appendLine()
            append(source)
            appendLine()
            appendLine()
            append(TRANSFORM_HEADER)
            appendLine()
            appendLine()
            append(transformed)
            appendLine()
        }

    companion object {
        val SOURCE_HEADER = """
            //
            // Source
            // ------------------------------------------
        """.trimIndent()
        val TRANSFORM_HEADER = """
            //
            // Transformed IR
            // ------------------------------------------
        """.trimIndent()

        fun fromEncodedString(encoded: String): GoldenTransformTestInfo {
            val split = encoded.removePrefix(SOURCE_HEADER).split(TRANSFORM_HEADER)
            if (split.size != 2) {
                error("Could not parse encoded golden string. " +
                    "Expected 2 sections but was ${split.size}.")
            }
            return GoldenTransformTestInfo(split[0].trim(), split[1].trim())
        }
    }
}

private fun String.quotedSplit(): List<String> {
    val result = mutableListOf<String>()
    var current = 0

    while (current < length) {
        var start = current
        var end: Int
        when (get(current)) {
            ' ', '\n', '\r', ',' -> {
                current++
                continue
            }
            '"' -> {
                start = ++current
                while (current < length && get(current) != '"') {
                    current++
                }
                end = current++
            }
            else -> {
                while (current < length && get(current) != ',') {
                    current++
                }
                end = current++
            }
        }
        result.add(substring(start, end))
    }
    return result
}
