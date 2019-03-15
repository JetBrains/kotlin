package samples.text

import samples.*

class Regexps {

    @Sample
    fun matchDestructuringToGroupValues() {
        val inputString = "John 9731879"
        val match = Regex("(\\w+) (\\d+)").find(inputString)!!
        val (name, phone) = match.destructured

        assertPrints(name, "John")     // value of the first group matched by \w+
        assertPrints(phone, "9731879") // value of the second group matched by \d+

        // group with the zero index is the whole substring matched by the regular expression
        assertPrints(match.groupValues, "[John 9731879, John, 9731879]")

        val numberedGroupValues = match.destructured.toList()
        // destructured group values only contain values of the groups, excluding the zeroth group.
        assertPrints(numberedGroupValues, "[John, 9731879]")
    }

    @Sample
    fun find() {
        val inputString = "to be or not to be"
        val regex = "to \\w{2}".toRegex()
        // If there is matching string, then find method returns non-null MatchResult
        val match = regex.find(inputString)!!
        assertPrints(match.value, "to be")
        assertPrints(match.range, "0..4")

        val nextMatch = match.next()!!
        assertPrints(nextMatch.range, "13..17")

        val regex2 = "this".toRegex()
        // If there is no matching string, then find method returns null
        assertPrints(regex2.find(inputString), "null")

        val regex3 = regex
        // to be or not to be
        //              ^^^^^
        // Because the search starts from the index 2, it finds the last "to be".
        assertPrints(regex3.find(inputString, 2)!!.range, "13..17")
    }

    @Sample
    fun findAll() {
        val text = "Hello Alice. Hello Bob. Hello Eve."
        val regex = Regex("Hello (.*?)[.]")
        val matches = regex.findAll(text)
        val names = matches.map { it.groupValues[1] }.joinToString()
        assertPrints(names, "Alice, Bob, Eve")
    }
}