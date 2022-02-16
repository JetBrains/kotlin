/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.getMergedScriptText
import kotlin.script.experimental.host.toScriptSource

@RunWith(Parameterized::class)
class ScriptHostUtilTest : TestCase() {

    @Parameterized.Parameter(0)
    lateinit var testName: String

    @Parameterized.Parameter(1)
    lateinit var params: MergedScriptTextTestParams

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = listOf(
            arrayOf("first and last fragments included", MergedScriptTextTestParams(listOf(0, 2))),
            arrayOf("first fragment is included and last is excluded", MergedScriptTextTestParams(listOf(0, 1))),
            arrayOf("first fragment is excluded and last is included", MergedScriptTextTestParams(listOf(1, 2))),
            arrayOf("first and last fragments are excluded", MergedScriptTextTestParams(listOf(1))),

            arrayOf("first and last fragments included, random char pool", MergedScriptTextTestParams(listOf(0, 2), randomCharPool = true)),
            arrayOf(
                "first fragment is included and last is excluded, random char pool",
                MergedScriptTextTestParams(listOf(0, 1), randomCharPool = true)
            ),
            arrayOf(
                "first fragment is excluded and last is included, random char pool",
                MergedScriptTextTestParams(listOf(1, 2), randomCharPool = true)
            ),
            arrayOf(
                "first and last fragments are excluded, random char pool",
                MergedScriptTextTestParams(listOf(1), randomCharPool = true)
            ),

            arrayOf("first and last fragments included, with multiple lines", MergedScriptTextTestParams(listOf(0, 2), "a\nb\nc")),
            arrayOf("first included, last excluded, with multiple lines", MergedScriptTextTestParams(listOf(0, 1), "a\nb\nc")),
            arrayOf("first excluded, last included, with multiple lines", MergedScriptTextTestParams(listOf(1, 2), "a\nb\nc")),
            arrayOf("first and last fragments are excluded, with multiple lines", MergedScriptTextTestParams(listOf(1), "a\nb\nc")),

            arrayOf("first and last fragments included, duplicate fragments", MergedScriptTextTestParams(listOf(0, 2), "aaa")),
            arrayOf("first included, last excluded, duplicate fragments", MergedScriptTextTestParams(listOf(0, 1), "aaa")),
            arrayOf("first is excluded,last is included, duplicate fragments", MergedScriptTextTestParams(listOf(1, 2), "aaa")),
            arrayOf("first and last fragments are excluded, duplicate fragments", MergedScriptTextTestParams(listOf(1), "aaa")),
        )
    }


    data class MergedScriptTextTestParams(
        val fragmentsToInclude: List<Int>,
        val intiCharPool: CharSequence = "abc",
        val randomCharPool: Boolean = false
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
            val textRange: SourceCode.Range
        ) {
            fun toScriptSourceNamedFragment(index: Int) = ScriptSourceNamedFragment("fragment$index", textRange)
        }


        fun addRandomFragment(): FragmentedText {
            val randomText = (1..28).joinToString(separator = "") { charPool.random().toString() }
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

    @Test
    fun `test `() {

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
        lines: Int = charSeq.length
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
        assertEquals("Line count differ.", originalText.lines().count(), mergedText.lines().count())
        // Check that char count is the same
        assertEquals("Char count differ.", originalText.length, mergedText.length)
        includedFragments.forEach {
            // Check that every included fragment is included in correct position
            assertEquals(
                "Incorrect fragment in position.",
                it.text,
                mergedText.substring(it.textRange.start.absolutePos!!, it.textRange.end.absolutePos!! - 1)
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
                "Either incorrect fragment was included OR excluded fragments were not cleaned properly.",
                it.matches("\\s*".toRegex())
            )
        }
    }

}

