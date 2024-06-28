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

import androidx.compose.compiler.plugins.kotlin.analysis.StabilityConfigParser
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StabilityConfigurationParserTests {
    private fun testConfigParsing(config: String, expectedClasses: Set<String>) {
        val parser = StabilityConfigParser.fromLines(config.lines())
        assertEquals(expectedClasses, parser.stableTypeMatchers.map { it.pattern }.toSet())
    }

    private fun testConfigParsingThrows(config: String) {
        assertThrows(IllegalStateException::class.java) {
            StabilityConfigParser.fromLines(config.lines())
        }
    }

    @Test
    fun testSingleLineClassName() = testConfigParsing(
        """
            com.foo.bar
        """.trimIndent(),
        setOf("com.foo.bar")
    )

    @Test
    fun testSingleLineClassNameWithNonAlphaCharacters() = testConfigParsing(
        """
            com.foo_1.bar
        """.trimIndent(),
        setOf("com.foo_1.bar")
    )

    @Test
    fun testMultipleClassNames() = testConfigParsing(
        """
            com.foo.bar
            com.bar.foo
        """.trimIndent(),
        setOf("com.foo.bar", "com.bar.foo")
    )

    @Test
    fun testCommentsAreIgnored() = testConfigParsing(
        """
            // Comment first line
            com.foo.bar
            // Comment last line
        """.trimIndent(),
        setOf("com.foo.bar")
    )

    @Test
    fun whitespaceIgnored() = testConfigParsing(
        """

               com.foo.bar

        """.trimIndent(),
        setOf("com.foo.bar")
    )

    @Test
    fun testWildcardsAreParsed() = testConfigParsing(
        """
            // Comment first line
            com.*
            // Comment last line
        """.trimIndent(),
        setOf("com.*")
    )

    @Test
    fun testWildcardInMiddle() = testConfigParsing(
        """
            com.*.bar
        """.trimIndent(),
        setOf("com.*.bar")
    )

    @Test
    fun testMultipleWildcardInMiddle() = testConfigParsing(
        """
            com.*.bar.*
        """.trimIndent(),
        setOf("com.*.bar.*")
    )

    @Test
    fun testWhitespaceThrows() = testConfigParsingThrows(
        """
            com. ab.*
        """.trimIndent()
    )

    @Test
    fun testInlineCommentThrows() = testConfigParsingThrows(
        """
            com.foo.* //comment
        """.trimIndent()
    )

    @Test
    fun testIllegalCharacterThrows() = testConfigParsingThrows(
        """
            com.foo!.bar //comment
        """.trimIndent()
    )
}

private const val PATH_TO_CONFIG_FILES = "$TEST_RESOURCES_ROOT/testStabilityConfigFiles"
class SingleStabilityConfigurationTest(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.STABILITY_CONFIG_PATH_KEY,
            listOf("$PATH_TO_CONFIG_FILES/config1.conf")
        )
        put(ComposeConfiguration.STRONG_SKIPPING_ENABLED_KEY, false)
    }

    @Test
    fun testExternalTypeStable() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable
            import java.time.Instant

            @Composable
            fun SkippableComposable(list: List<String>) {
                use(list)
            }

            @Composable
            fun UnskippableComposable(instant: Instant) {
                use(instant)
            }
        """.trimIndent(),
        extra = """
            fun use(foo: Any) {}
        """.trimIndent()
    )
}

class MultipleStabilityConfigurationTest(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.STABILITY_CONFIG_PATH_KEY,
            listOf(
                "$PATH_TO_CONFIG_FILES/config1.conf",
                "$PATH_TO_CONFIG_FILES/config2.conf"
            )
        )
        put(
            ComposeConfiguration.FEATURE_FLAGS,
            listOf(FeatureFlag.OptimizeNonSkippingGroups.featureName)
        )
    }

    @Test
    fun testExternalTypeStable() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable
            import java.time.Instant

            @Composable
            fun SkippableComposable(list: List<String>, instant: Instant) {
                use(list)
                use(instant)
            }
        """.trimIndent(),
        extra = """
            fun use(foo: Any) {}
        """.trimIndent()
    )
}
