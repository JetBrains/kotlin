package samples.text

import samples.*
import kotlin.test.*
import java.util.*

class Strings {

    @Sample
    fun capitalize() {
        assertPrints("abcd".capitalize(), "Abcd")
        assertPrints("Abcd".capitalize(), "Abcd")
    }

    @Sample
    fun decaptialize() {
        assertPrints("abcd".decapitalize(), "abcd")
        assertPrints("Abcd".decapitalize(), "abcd")
    }

    @Sample
    fun repeat() {
        assertPrints("Word".repeat(4), "WordWordWordWord")
        assertPrints("Word".repeat(0), "")
    }

    @Sample
    fun trimIndent() {
        val withoutIndent =
                """
                    ABC
                    123
                    456
                """.trimIndent()
        assertPrints(withoutIndent, "ABC\n123\n456")
    }

    @Sample
    fun trimMargin() {
        val withoutMargin1 = """ABC
                        |123
                        |456""".trimMargin()
        assertPrints(withoutMargin1, "ABC\n123\n456")

        val withoutMargin2 = """
            #XYZ
            #foo
            #bar
        """.trimMargin("#")
        assertPrints(withoutMargin2, "XYZ\nfoo\nbar")
    }


}