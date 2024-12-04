/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class FixedLengthQuantifierTest {
    companion object {
        private val quantifierMatchCount = 100_000
        private val compositeMin = 50_000
        private val compositeMax = 150_000

        private val input = "a".repeat(quantifierMatchCount)
        private val inputDescription = "\"a\".repeat($quantifierMatchCount)"
    }

    private fun testMatches(regex: Regex, input: String, inputDescription: String, expected: Boolean = true) {
        val message = "$regex should ${if (expected) "" else "not " }match $inputDescription"
        assertEquals(expected, regex.matches(input), message)
    }

    @Test
    fun fixedLengthQualifierGreedy() {
        val plusRegex = Regex("[^\\s]+")
        testMatches(plusRegex, input, inputDescription)

        val starRegex = Regex("[^\\s]*")
        testMatches(starRegex, input, inputDescription)

        Regex("[^\\s\\d]{$compositeMin,$compositeMax}").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex("[^\\s\\d]{$compositeMin,$quantifierMatchCount}").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex("[^\\s\\d]{$compositeMin,${quantifierMatchCount - 1}}").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription, expected = false)
        }
        Regex("[^\\s\\d]{$quantifierMatchCount,$compositeMax}").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex("[^\\s\\d]{${quantifierMatchCount + 1},$compositeMax}").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription, expected = false)
        }
    }

    @Test
    fun fixedLengthQualifierReluctant() {
        val plusRegex = Regex(".+?")
        testMatches(plusRegex, input, inputDescription)

        val starRegex = Regex(".*?")
        testMatches(starRegex, input, inputDescription)

        Regex(".{$compositeMin,$compositeMax}?").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex(".{$compositeMin,$quantifierMatchCount}?").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex(".{$compositeMin,${quantifierMatchCount - 1}}?").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription, expected = false)
        }
        Regex(".{$quantifierMatchCount,$compositeMax}?").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex(".{${quantifierMatchCount + 1},$compositeMax}?").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription, expected = false)
        }
    }

    @Test
    fun fixedLengthQualifierPossesive() {
        val plusRegex = Regex("\\p{Ll}++")
        testMatches(plusRegex, input, inputDescription)

        val starRegex = Regex("\\p{Ll}*+")
        testMatches(starRegex, input, inputDescription)

        Regex("\\p{Ll}{$compositeMin,$compositeMax}+").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex("\\p{Ll}{$compositeMin,$quantifierMatchCount}+").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex("\\p{Ll}{$compositeMin,${quantifierMatchCount - 1}}+").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription, expected = false)
        }
        Regex("\\p{Ll}{$quantifierMatchCount,$compositeMax}+").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription)
        }
        Regex("\\p{Ll}{${quantifierMatchCount + 1},$compositeMax}+").let { compositeRegex ->
            testMatches(compositeRegex, input, inputDescription, expected = false)
        }
    }

    @Test
    fun leafQuantifierGreedy() {
        val plusRegex = Regex("a+")
        testMatches(plusRegex, input, inputDescription)

        val starRegex = Regex("a*")
        testMatches(starRegex, input, inputDescription)

        val compositeRegex = Regex("a{$compositeMin,$compositeMax}")
        testMatches(compositeRegex, input, inputDescription)
    }

    @Test
    fun kt46211_space() {
        val regex = "(https?|ftp)://[^\\s/$.?#].[^\\s]*".toRegex(RegexOption.IGNORE_CASE)
        val link = "http://" + input
        testMatches(regex, link, "\"http://\" + $inputDescription")
    }

    @Test
    fun kt46211() {
        val regex = Regex("[a]+")
        val output = regex.replace(input, "")
        assertEquals("", output)
    }

    @Test
    fun kt53352() {
        val test = input + "b c"
        val regex = """(.*?b.*?c)""".toRegex()
        val res = regex.find(test)!!
        assertEquals(test, res.groupValues[1])
    }

    @Test
    fun kt35508() {
        val doesNotWork = """=== EREIGNISLISTE ======
""" + "\u001b" + """Kn
BEGINN       28.06 13:25
EREIGNISSE            62
50 5          28.06 1325
3402          28.06 1325
3412          28.06 1325
63 3          28.06 1325
63 0          28.06 1325
EE06          28.06 1325
EE06          28.06 1322
EE07          28.06 1322
63 3          28.06 1322
EE06          28.06 1322
EE07          28.06 1322
63 3          28.06 1322
63 3          28.06 1322
63 3          28.06 1323
63 3          28.06 1500
50 4          28.06 1500
50 5          30.06 1226
3402          30.06 1226
3412          30.06 1226
50 4          30.06 1227
50 5          30.06 1228
3402          30.06 1228"""

        val regex = Regex("(\\x1b\\w[\\s\\S]{1,2})([\\s\\S]+?(?=\\x1b\\w[\\s\\S]{1,2}|\$))")

        fun regexTest(content: String): List<String> {
            return regex.findAll(content).map {
                it.groupValues[1]
            }.toList()
        }

        assertEquals("\u001BKn\n", regexTest(doesNotWork).single())
    }
}