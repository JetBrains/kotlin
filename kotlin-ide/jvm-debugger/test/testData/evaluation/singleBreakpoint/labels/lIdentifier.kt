package lIdentifier

fun main(args: Array<String>) {
    val a = "1"
    val b = "1"
    val c = "1"
    val d = "1"
    val e = "1"
    //Breakpoint!
    val f = 1
}

// DEBUG_LABEL: a = 0
// DEBUG_LABEL: b = <no_name>
// DEBUG_LABEL: c = for
// DEBUG_LABEL: d = *
// DEBUG_LABEL: e = e-e

// EXPRESSION: a
// RESULT: "1": Ljava/lang/String;