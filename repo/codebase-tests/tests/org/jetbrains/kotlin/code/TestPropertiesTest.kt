/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.ExperimentalKotlinTestApi
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [TESTS_PATH] contains a `TestCompilePaths` object that declares properties used in our tests.
 * [GRADLE_PATH] contains a `TestCompilePaths` object that exposes the same properties for the build,
 * so one can use [GRADLE_PATH] to set the property value and [TESTS_PATH] to read it.
 *
 * Both of these objects should contain the same list of properties with the same values. This test ensures
 * that they are indeed the same.
 */
class TestPropertiesTest {
    companion object {
        const val GRADLE_PATH = "repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/TestCompilePaths.kt"
        const val TESTS_PATH =
            "compiler/test-infrastructure-utils/testFixtures/org/jetbrains/kotlin/codegen/forTestCompile/TestCompilePaths.kt"

        val TEST_COMPILE_PATHS_REGEX = Regex("""object\s+TestCompilePaths\s+\{((\s*.*)*)}""")
        val TEST_COMPILE_PATHS_PROPERTY_REGEX = Regex("""\s*const\s+val\s+([A-Z_\d]+)(\s*:\s+String)?\s*=\s*"(.*?)"\s*""")
    }

    private fun Path.extractTestCompilePathsProperties(): Map<String, String> {
        val propertyLines = TEST_COMPILE_PATHS_REGEX.find(readText())?.groupValues?.getOrNull(1)
            ?: fail("TestCompilePaths object not found in '$this'")
        return propertyLines.lineSequence().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val property = TEST_COMPILE_PATHS_PROPERTY_REGEX.find(line)?.let { match ->
                match.groupValues[1] to match.groupValues[3]
            }
            property ?: fail("'$this' contains unexpected property line: '$line'")
        }.toMap()
    }

    @OptIn(ExperimentalKotlinTestApi::class)
    @Test
    fun `ensure TestCompilePaths are the same`() {
        val gradleProperties = Path.of(GRADLE_PATH).extractTestCompilePathsProperties()
        val codebaseProperties = Path.of(TESTS_PATH).extractTestCompilePathsProperties()

        val allProperties = gradleProperties.keys.toSet() + codebaseProperties.keys.toSet()

        val diff = mutableListOf<String>()
        for (property in allProperties) {
            val gradleValue = gradleProperties[property]
            val codebaseValue = codebaseProperties[property]

            if (gradleValue == null) {
                diff += "`${property}` is not declared in repo/gradle-build-conventions/project-tests-convention"
            } else if (codebaseValue == null) {
                diff += "`${property}` is not declared in compiler/test-infrastructure-utils/testFixtures"
            } else if (gradleValue != codebaseValue) {
                diff += "`${property}`: values are different: \"$codebaseValue\" != \"$gradleValue\""
            }
        }
        assertTrue(diff.isEmpty()) {
            "TestCompilePaths.kt in $TESTS_PATH and $GRADLE_PATH are different:\n${diff.joinToString("\n")}"
        }
    }
}
