package samples.text

import samples.*
import kotlin.test.*
import java.util.*

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
        val regex = ".*be".toRegex()
        // If there is matching string, then find method returns MatchResult
        assertTrue(regex.find(inputString) != null)

        val regex2 = ".*this.*".toRegex()
        // If there is no matching string, then find method returns null
        assertFalse(regex2.find(inputString) != null)

        val regex3 = "to.*".toRegex()
        // to be or not to be
        //              ^^^^^
        // Because it starts from index 1, find method finds "to be" not the whole sentence.
        assertPrints(regex3.find(inputString, 1)?.value, "to be")
    }
}