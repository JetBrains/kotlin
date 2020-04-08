// FILE: severalInlineCallsFromOtherFile.kt
package severalInlineCallsFromOtherFile

fun main(args: Array<String>) {
    //Breakpoint!
    firstCall()
    secondCall()
}

fun firstCall() {
    inlineFun()
}

fun secondCall() {
    inlineFun()
}

// RESUME: 2
// ADDITIONAL_BREAKPOINT: severalInlineCallsFromOtherFile.Other.kt: Breakpoint 1

// FILE: severalInlineCallsFromOtherFile.Other.kt
package severalInlineCallsFromOtherFile

inline fun inlineFun() {
    var i = 1
    // Breakpoint 1
    i++
    i++
}