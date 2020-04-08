package lSimple

fun main(args: Array<String>) {
    val a = "str"
    //Breakpoint!
    val b = 1
}

// DEBUG_LABEL: a = aLabel

// EXPRESSION: a
// RESULT: "str": Ljava/lang/String;

// EXPRESSION: aLabel_DebugLabel
// RESULT: "str": Ljava/lang/String;