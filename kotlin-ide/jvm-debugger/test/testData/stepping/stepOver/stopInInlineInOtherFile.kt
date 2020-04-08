// FILE: stopInInlineInOtherFile.kt
package stopInInlineInOtherFile

fun main(args: Array<String>) {
    inlineFun()
}

// ADDITIONAL_BREAKPOINT: stopInInlineInOtherFile.Other.kt: Breakpoint 1

// FILE: stopInInlineInOtherFile.Other.kt
package stopInInlineInOtherFile

inline fun inlineFun() {
    var i = 1
    // Breakpoint 1
    i++
    i++
}