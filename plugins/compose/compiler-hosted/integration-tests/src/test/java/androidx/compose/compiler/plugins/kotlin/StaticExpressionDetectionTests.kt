/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import androidx.compose.compiler.plugins.kotlin.lower.dumpSrc
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class StaticExpressionDetectionTests : AbstractIrTransformTest() {

    @Test
    fun testUnstableTypesAreNeverStatic() = assertUnstable(
        expression = "Any()"
    )

    @Test
    fun testPrimitiveLiteralsAreStatic() = assertStatic(
        expression = "4"
    )

    @Test
    fun testKotlinArithmeticOperationsAreStatic() = assertStatic(
        expression = "(1f + 3f) / 2"
    )

    @Test
    fun testConstValReferencesAreStatic() = assertStatic(
        expression = "Constant",
        extraSrc = """
            const val Constant = "Hello world!"
        """
    )

    @Test
    fun testComputedValReferencesAreNotStatic() = assertStableAndNotStatic(
        expression = "computedProperty",
        extraSrc = """
            val computedProperty get() = 42
        """
    )

    @Test
    fun testVarReferencesAreNotStatic() = assertStableAndNotStatic(
        expression = "mutableProperty",
        extraSrc = """
            var mutableProperty = 42
        """
    )

    @Test
    fun testObjectReferencesAreStatic() = assertStatic(
        expression = "Singleton",
        extraSrc = """
            object Singleton
        """
    )

    @Test
    fun testStableFunctionCallsWithStaticParametersAreStatic() = assertStatic(
        expression = "stableFunction(42)",
        extraSrc = """
            import androidx.compose.runtime.Stable

            @Stable
            fun stableFunction(x: Int) = x.toString()
        """
    )

    @Test
    fun testListOfWithStaticParametersIsStatic() = assertStatic(
        expression = "listOf('a', 'b', 'c')"
    )

    @Test
    fun testEmptyListIsStatic() = assertStatic(
        expression = "emptyList<Any?>()"
    )

    @Test
    fun testMapOfWithStaticParametersIsStatic() = assertStatic(
        expression = "mapOf(pair)",
        extraSrc = """
            val pair = "answerToUltimateQuestion" to 42
        """
    )

    @Test
    fun testEmptyMapIsStatic() = assertStatic(
        expression = "emptyMap<Any, Any?>()"
    )

    @Test
    fun testPairsAreStatic() = assertStatic(
        expression = "'a' to 1"
    )

    @Test
    fun testEnumReferencesAreStatic() = assertStatic(
        expression = "Foo.Bar",
        extraSrc = """
            enum class Foo {
                Bar,
                Bam
            }
        """
    )

    @Test
    fun testDpLiteralsAreStatic() = assertStatic(
        expression = "Dp(4f)"
    )

    @Test
    fun testDpArithmeticIsStatic() = assertStatic(
        expression = "2 * 4.dp"
    )

    @Test
    fun testModifierReferenceIsStatic() = assertStatic(
        expression = "Modifier"
    )

    @Test
    fun testAlignmentReferenceIsStatic() = assertStatic(
        expression = "Alignment.Center"
    )

    @Test
    fun testContentScaleReferenceIsStatic() = assertStatic(
        expression = "ContentScale.Fit"
    )

    @Test
    fun testDefaultTextStyleReferenceIsStatic() = assertStatic(
        expression = "TextStyle.Default"
    )

    @Test
    fun testTextVisualTransformationNoneReferenceIsStatic() = assertStatic(
        expression = "VisualTransformation.None"
    )

    @Test
    fun testDefaultKeyboardActionsIsStatic() = assertStatic(
        expression = "KeyboardActions.Default"
    )

    @Test
    fun testDefaultKeyboardOptionsIsStatic() = assertStatic(
        expression = "KeyboardOptions.Default"
    )

    @Test
    fun testKeyboardOptionsWithLiteralsIsStatic() = assertStatic(
        expression = "KeyboardOptions(autoCorrect = false)"
    )

    @Test
    fun testUnspecifiedColorIsStatic() = assertStatic(
        expression = "Color.Unspecified"
    )

    @Test
    fun testUnspecifiedDpIsStatic() = assertStatic(
        expression = "Dp.Unspecified"
    )

    @Test
    fun testUnspecifiedTextUnitIsStatic() = assertStatic(
        expression = "TextUnit.Unspecified"
    )

    @Test
    fun testPaddingValuesZeroIsStatic() = assertStatic(
        expression = "PaddingValues()"
    )

    @Test
    fun testPaddingValuesAllIsStatic() = assertStatic(
        expression = "PaddingValues(all = 16.dp)"
    )

    @Test
    fun testEmptyCoroutineContextIsStatic() = assertStatic(
        expression = "EmptyCoroutineContext"
    )

    private fun assertStatic(
        expression: String,
        @Language("kotlin")
        extraSrc: String = ""
    ) {
        assertParameterChangeBitsForExpression(
            message = "Expression `$expression` did not compile with the correct %changed flags",
            expression = expression,
            extraSrc = extraSrc,
            expectedEncodedChangedParameter = ChangedParameterEncoding.Static
        )
    }

    private fun assertStableAndNotStatic(
        expression: String,
        @Language("kotlin")
        extraSrc: String = ""
    ) {
        assertParameterChangeBitsForExpression(
            message = "Expression `$expression` did not compile with the correct %changed flags",
            expression = expression,
            extraSrc = extraSrc,
            expectedEncodedChangedParameter = ChangedParameterEncoding.Uncertain
        )
    }

    private fun assertUnstable(
        expression: String,
        @Language("kotlin")
        extraSrc: String = ""
    ) {
        assertParameterChangeBitsForExpression(
            message = "Expression `$expression` did not compile with the correct %changed flags",
            expression = expression,
            extraSrc = extraSrc,
            expectedEncodedChangedParameter = ChangedParameterEncoding.Unstable
        )
    }

    private fun assertParameterChangeBitsForExpression(
        message: String,
        expression: String,
        expectedEncodedChangedParameter: ChangedParameterEncoding,
        @Language("kotlin")
        extraSrc: String = ""
    ) {
        @Language("kotlin")
        val source = """
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp
            import androidx.compose.ui.unit.TextUnit
            import androidx.compose.ui.unit.times
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.layout.ContentScale
            import androidx.compose.ui.text.TextStyle
            import androidx.compose.ui.text.input.VisualTransformation
            import androidx.compose.foundation.text.KeyboardActions
            import androidx.compose.foundation.text.KeyboardOptions
            import androidx.compose.foundation.layout.PaddingValues
            import kotlin.coroutines.EmptyCoroutineContext

            @Composable fun Receiver(value: Any?) {}

            @Composable fun CompositionContext() {
                Receiver(value = $expression)
            }
        """.trimIndent()

        val files = listOf(
            SourceFile("ExtraSrc.kt", extraSrc),
            SourceFile("Test.kt", source),
        )
        val irModule = compileToIr(files)

        val changeFlagsMatcher = Regex(
            pattern = """Receiver\(.+, %composer, (0b)?([01]+)\)""",
            option = RegexOption.DOT_MATCHES_ALL
        )
        val compositionContextBody = irModule.files.last().declarations
            .filterIsInstance<IrFunction>()
            .first { it.name.identifier == "CompositionContext" }
            .dumpSrc()
            .replace('$', '%')

        assertChangedBits(
            message = message,
            expected = expectedEncodedChangedParameter,
            actual = checkNotNull(
                changeFlagsMatcher.find(compositionContextBody)?.groupValues?.last()
                    ?.toInt(radix = 2)
                    ?.let { (it shr 1) and ChangedParameterEncoding.Mask }
            ) {
                "Failed to resolve %changed flags for expression `$expression`."
            }
        )
    }

    private fun assertChangedBits(
        message: String,
        expected: ChangedParameterEncoding,
        actual: Int
    ) {
        val maskedActual = actual and ChangedParameterEncoding.Mask
        if (ChangedParameterEncoding.values().none { it.bits == maskedActual }) {
            fail("$message\nThe actual %changed flags contained an illegal encoding: " +
                "0b${maskedActual.toString(radix = 2)}")
        }

        assertEquals(
            message,
            expected.bits.toString(radix = 2).padStart(length = 3, '0'),
            maskedActual.toString(radix = 2).padStart(length = 3, '0')
        )
    }

    private enum class ChangedParameterEncoding(val bits: Int) {
        Uncertain(0b000),
        Same(0b001),
        Different(0b010),
        Static(0b011),
        Unstable(0b100),

        ;

        companion object {
            const val Mask = 0b111
        }
    }
}