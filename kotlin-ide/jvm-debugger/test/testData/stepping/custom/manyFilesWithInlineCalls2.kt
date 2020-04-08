// FILE: manyFilesWithInlineCalls2.kt
package manyFilesWithInlineCalls2

import manyFilesWithInlineCalls2.first.*
import manyFilesWithInlineCalls2.second.*

fun main(args: Array<String>) {
    secondInline()
}

fun unused() {
    firstInline()
}

// ADDITIONAL_BREAKPOINT: manyFilesWithInlineCalls2.Second.kt: Breakpoint 1

// FILE: manyFilesWithInlineCalls2.First.kt
package manyFilesWithInlineCalls2.first

inline fun firstInline() {
    1 + 1
}

// FILE: manyFilesWithInlineCalls2.Second.kt
package manyFilesWithInlineCalls2.second

inline fun secondInline() {
    // Breakpoint 1
    1 + 1
}