// FILE: manyFilesWithInlineCalls1.kt
package manyFilesWithInlineCalls1

import manyFilesWithInlineCalls1.first.*
import manyFilesWithInlineCalls1.second.*

fun main(args: Array<String>) {
    firstInline()
}

fun unused() {
    secondInline()
}

// ADDITIONAL_BREAKPOINT: manyFilesWithInlineCalls1.First.kt: Breakpoint 1

// FILE: manyFilesWithInlineCalls1.First.kt
package manyFilesWithInlineCalls1.first

inline fun firstInline() {
    // Breakpoint 1
    1 + 1
}

// FILE: manyFilesWithInlineCalls1.Second.kt
package manyFilesWithInlineCalls1.second

inline fun secondInline() {
    1 + 1
}
