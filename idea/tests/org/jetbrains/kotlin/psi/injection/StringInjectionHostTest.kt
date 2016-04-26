/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.injection

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import java.util.*

class StringInjectionHostTest : KotlinTestWithEnvironment() {
    fun testRegular() {
        with (quoted("")) {
            checkInjection("", mapOf(0 to 1))
            assertOneLine()
        }
        with (quoted("a")) {
            checkInjection("a", mapOf(0 to 1, 1 to 2))
            assertOneLine()
        }
        with (quoted("ab")) {
            checkInjection("ab", mapOf(0 to 1, 1 to 2, 2 to 3))
            checkInjection("a", mapOf(0 to 1, 1 to 2), rangeInHost = TextRange(1, 2))
            checkInjection("b", mapOf(0 to 2, 1 to 3), rangeInHost = TextRange(2, 3))
            assertOneLine()
        }
    }

    fun testUnclosedSimpleLiteral() {
        assertFalse(stringExpression("\"").isValidHost)
        assertFalse(stringExpression("\"a").isValidHost)
    }

    fun testEscapeSequences() {
        with (quoted("\\t")) {
            checkInjection("\t", mapOf(0 to 1, 1 to 3))
            assertNoInjection(TextRange(1, 2))
            assertNoInjection(TextRange(2, 3))
            assertOneLine()
        }

        with (quoted("a\\tb")) {
            checkInjection("a\tb", mapOf(0 to 1, 1 to 2, 2 to 4, 3 to 5))
            checkInjection("a", mapOf(0 to 1, 1 to 2), rangeInHost = TextRange(1, 2))
            assertNoInjection(TextRange(1, 3))
            checkInjection("a\t", mapOf(0 to 1, 1 to 2, 2 to 4), rangeInHost = TextRange(1, 4))
            checkInjection("\t", mapOf(0 to 2, 1 to 4), rangeInHost = TextRange(2, 4))
            checkInjection("\tb", mapOf(0 to 2, 1 to 4, 2 to 5), rangeInHost = TextRange(2, 5))
            assertOneLine()
        }
    }

    fun testTripleQuotes() {
        with (tripleQuoted("")) {
            checkInjection("", mapOf(0 to 3))
            assertMultiLine()
        }
        with (tripleQuoted("a")) {
            checkInjection("a", mapOf(0 to 3, 1 to 4))
            assertMultiLine()
        }
        with (tripleQuoted("ab")) {
            checkInjection("ab", mapOf(0 to 3, 1 to 4, 2 to 5))
            checkInjection("a", mapOf(0 to 3, 1 to 4), rangeInHost = TextRange(3, 4))
            checkInjection("b", mapOf(0 to 4, 1 to 5), rangeInHost = TextRange(4, 5))
            assertMultiLine()
        }
    }

    fun testEscapeSequenceInTripleQuotes() {
        with (tripleQuoted("\\t")) {
            checkInjection("\\t", mapOf(0 to 3, 1 to 4, 2 to 5))
            checkInjection("\\", mapOf(0 to 3, 1 to 4), rangeInHost = TextRange(3, 4))
            checkInjection("t", mapOf(0 to 4, 1 to 5), rangeInHost = TextRange(4, 5))
            assertMultiLine()
        }
    }

    fun testMultiLine() {
        with (tripleQuoted("a\nb")) {
            checkInjection("a\nb", mapOf(0 to 3, 1 to 4, 2 to 5, 3 to 6))
            assertMultiLine()
        }
    }

    private fun quoted(s: String): KtStringTemplateExpression {
        return stringExpression("\"$s\"")
    }

    private fun tripleQuoted(s: String): KtStringTemplateExpression {
        return stringExpression("\"\"\"$s\"\"\"")
    }

    private fun stringExpression(s: String): KtStringTemplateExpression {
        return KtPsiFactory(project).createExpression(s) as KtStringTemplateExpression
    }

    private fun KtStringTemplateExpression.assertNoInjection(range: TextRange): KtStringTemplateExpression {
        assertTrue(isValidHost)
        assertFalse(createLiteralTextEscaper().decode(range, StringBuilder()))
        return this
    }

    private fun KtStringTemplateExpression.assertOneLine() {
        assertTrue(createLiteralTextEscaper().isOneLine)
    }

    private fun KtStringTemplateExpression.assertMultiLine() {
        assertFalse(createLiteralTextEscaper().isOneLine)
    }

    private fun KtStringTemplateExpression.checkInjection(
            decoded: String, targetToSourceOffsets: Map<Int, Int>, rangeInHost: TextRange? = null
    ) {
        assertTrue(isValidHost)
        for (prefix in listOf("", "prefix")) {
            val escaper = createLiteralTextEscaper()
            val chars = StringBuilder(prefix)
            val range = rangeInHost ?: escaper.relevantTextRange
            assertTrue(escaper.decode(range, chars))
            assertEquals(decoded, chars.substring(prefix.length))
            val extendedOffsets = HashMap(targetToSourceOffsets)
            val beforeStart = targetToSourceOffsets.keys.min()!! - 1
            if (beforeStart >= 0) {
                extendedOffsets[beforeStart] = -1
            }
            extendedOffsets[targetToSourceOffsets.keys.max()!! + 1] = -1
            for ((target, source) in extendedOffsets) {
                assertEquals("Wrong source offset for $target", source, escaper.getOffsetInHost(target, range))
            }
        }
    }

    override fun createEnvironment() = createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY)
}
