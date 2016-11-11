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

        assertPrints(name, "John")
        assertPrints(phone, "9731879")

        assertPrints(match.groupValues, "[John 9731879, John, 9731879]")

        val numberedGroupValues = match.destructured.toList()
        assertPrints(numberedGroupValues, "[John, 9731879]")
    }
}