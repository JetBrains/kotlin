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
}