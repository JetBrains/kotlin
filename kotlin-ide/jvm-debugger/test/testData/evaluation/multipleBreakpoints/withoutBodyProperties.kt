package withoutBodyProperties

import kotlin.properties.Delegates

// EXPRESSION: 1 + 1
// RESULT: 2: I
//FieldWatchpoint! (a)
val a = 1

// EXPRESSION: 1 + 3
// RESULT: 4: I
//FieldWatchpoint! (aLambda)
val aLambda = { 1 + 1 }

class A {
    init {
        // EXPRESSION: i
        // RESULT: 1: I
        for (i in 1..1) {
            //Breakpoint!
            val a = 1
        }
    }

    // EXPRESSION: 1 + 6
    // RESULT: 7: I
    //FieldWatchpoint! (prop)
    val prop = 1

    fun test()= prop
}

fun main(args: Array<String>) {
    a
    aLambda

    A().test()
}

// WATCH_FIELD_INITIALISATION: true
// WATCH_FIELD_ACCESS: false
// WATCH_FIELD_MODIFICATION: false