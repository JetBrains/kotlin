/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.getMergedScriptText
import kotlin.script.experimental.host.toScriptSource

class ScriptHostUtilTest {

    companion object {
        @JvmStatic
        fun parameters() = listOf(
            Arguments.of(Named.of("first and last fragments included", MergedScriptTextTestParams(listOf(0, 2)))),
            Arguments.of(Named.of("first fragment is included and last is excluded", MergedScriptTextTestParams(listOf(0, 1)))),
            Arguments.of(Named.of("first fragment is excluded and last is included", MergedScriptTextTestParams(listOf(1, 2)))),
            Arguments.of(Named.of("first and last fragments are excluded", MergedScriptTextTestParams(listOf(1)))),

            Arguments.of(
                Named.of(
                    "first and last fragments included, random char pool",
                    MergedScriptTextTestParams(listOf(0, 2), randomCharPool = true)
                )
            ),
            Arguments.of(
                Named.of(
                    "first fragment is included and last is excluded, random char pool",
                    MergedScriptTextTestParams(listOf(0, 1), randomCharPool = true)
                )
            ),
            Arguments.of(
                Named.of(
                    "first fragment is excluded and last is included, random char pool",
                    MergedScriptTextTestParams(listOf(1, 2), randomCharPool = true)
                )
            ),
            Arguments.of(
                Named.of(
                    "first and last fragments are excluded, random char pool",
                    MergedScriptTextTestParams(listOf(1), randomCharPool = true)
                )
            ),

            Arguments.of(
                Named.of(
                    "first and last fragments included, with multiple lines",
                    MergedScriptTextTestParams(listOf(0, 2), "a\nb\nc")
                )
            ),
            Arguments.of(
                Named.of(
                    "first included, last excluded, with multiple lines",
                    MergedScriptTextTestParams(listOf(0, 1), "a\nb\nc")
                )
            ),
            Arguments.of(
                Named.of(
                    "first excluded, last included, with multiple lines",
                    MergedScriptTextTestParams(listOf(1, 2), "a\nb\nc")
                )
            ),
            Arguments.of(
                Named.of(
                    "first and last fragments are excluded, with multiple lines",
                    MergedScriptTextTestParams(listOf(1), "a\nb\nc")
                )
            ),

            Arguments.of(
                Named.of(
                    "first and last fragments included, duplicate fragments",
                    MergedScriptTextTestParams(listOf(0, 2), "aaa")
                )
            ),
            Arguments.of(Named.of("first included, last excluded, duplicate fragments", MergedScriptTextTestParams(listOf(0, 1), "aaa"))),
            Arguments.of(
                Named.of(
                    "first is excluded,last is included, duplicate fragments",
                    MergedScriptTextTestParams(listOf(1, 2), "aaa")
                )
            ),
            Arguments.of(
                Named.of(
                    "first and last fragments are excluded, duplicate fragments",
                    MergedScriptTextTestParams(listOf(1), "aaa")
                )
            ),
        )
    }


    data class MergedScriptTextTestParams(
        val fragmentsToInclude: List<Int>,
        val intiCharPool: CharSequence = "abc",
        val randomCharPool: Boolean = false,
    )

    class FragmentedText {
        private val sb: StringBuilder = StringBuilder()
        private val fragmentList: List<TextFragment> = mutableListOf()

        companion object {
            private val charPool: List<String> = ('a'..'z').map(Char::toString) + "\n"
        }

        fun fragments(): List<TextFragment> = fragmentList
        fun text(): String = sb.toString()

        data class TextFragment(
            val text: String,
            val textRange: SourceCode.Range,
        ) {
            fun toScriptSourceNamedFragment(index: Int) = ScriptSourceNamedFragment("fragment$index", textRange)
        }


        fun addRandomFragment(): FragmentedText {
            val randomText = (1..28).joinToString(separator = "") { charPool.random() }
            return addFragment(randomText)
        }

        fun addFragment(fragmentText: String): FragmentedText = apply {
            val fragmentTextLength = fragmentText.length
            val text = text()
            val fragmentStartLine: Int = text.lines().count()
            val fragmentEndLine = fragmentStartLine + fragmentText.count { it == '\n' }
            val fullTextLength = text.length
            val fragment = TextFragment(
                text = fragmentText,
                textRange = SourceCode.Range(
                    start = SourceCode.Position(fragmentStartLine, 1, fullTextLength),
                    end = SourceCode.Position(fragmentEndLine, fragmentTextLength + 1, fullTextLength + fragmentTextLength + 1)
                )
            )
            (fragmentList as MutableList<TextFragment>).add(fragment)
            sb.appendLine(fragmentText)
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    fun `test `(params: MergedScriptTextTestParams) {
        val fragmentedText = initFragmentedScript(params.intiCharPool, params.randomCharPool)
        val textFragments = fragmentedText.fragments()
        val includedFragments = params.fragmentsToInclude.map(textFragments::get)

        val script = fragmentedText.text().toScriptSource()
        val testConfig = scriptCompilationConfiguration(includedFragments)
        val mergedScriptText = getMergedScriptText(script, testConfig)
        runValidations(script.text, mergedScriptText, includedFragments)
    }

    private fun scriptCompilationConfiguration(includedFragments: List<FragmentedText.TextFragment>) =
        ScriptCompilationConfiguration {
            sourceFragments(
                includedFragments.mapIndexed { idx, textFragment ->
                    textFragment.toScriptSourceNamedFragment(idx)
                })
        }

    /**
     * Generates text
     * if randomCharPool is false from charSequence
     *  - for each char in [charSeq] it adds line of length of 3 to the generated text
     * if  randomCharPool is true
     *  - picks characters randomly, line count is [lines], line length is 28, some number bigger than alphabet size
     */
    private fun initFragmentedScript(
        charSeq: CharSequence = "abc",
        randomCharPool: Boolean,
        lines: Int = charSeq.length,
    ): FragmentedText {
        val generateFragment: FragmentedText.(Int) -> FragmentedText = when {
            randomCharPool -> { _ -> addRandomFragment() }
            else -> { line: Int ->
                val index = line % charSeq.length
                addFragment(charSeq[index].toString().repeat(3))
            }
        }

        return (1..lines).fold(FragmentedText()) { frag, line ->
            frag.generateFragment(line)
        }
    }

    private fun runValidations(originalText: String, mergedText: String, includedFragments: List<FragmentedText.TextFragment>) {
        // Check that line count is the same
        assertEquals(originalText.lines().count(), mergedText.lines().count(), "Line count differ.")
        // Check that char count is the same
        assertEquals(originalText.length, mergedText.length, "Char count differ.")
        includedFragments.forEach {
            // Check that every included fragment is included in correct position
            assertEquals(
                it.text,
                mergedText.substring(it.textRange.start.absolutePos!!, it.textRange.end.absolutePos!! - 1),
                "Incorrect fragment in position."
            )
        }
        // Check that after removing included fragments there are only whitespaces
        // meaning other fragments are excluded/cleaned
        includedFragments.fold(mergedText) { text, fragment ->
            text.replaceRange(
                fragment.textRange.start.absolutePos!!,
                fragment.textRange.end.absolutePos!!,
                " ".repeat(fragment.text.length + 1)
            )
        }.also {
            assertTrue(
                it.matches("\\s*".toRegex()),
                "Either incorrect fragment was included OR excluded fragments were not cleaned properly."
            )
        }
    }

}
