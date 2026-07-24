// KIND: STANDALONE
// MODULE: main(dependency)
// FILE: main.kt
import datetime.LocalDate

fun today(): LocalDate = LocalDate()

// MODULE: dependency
// FILE: dependency.kt
package datetime

class LocalDate {
    companion object {
        fun Format(block: (DateTimeFormatBuilder.WithDate) -> Unit): DateTimeFormat = TODO()
    }
}

interface DateTimeFormat

interface DateTimeFormatBuilder {
    interface WithDate
}
