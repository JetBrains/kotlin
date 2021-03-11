package lSeveralLabels

fun main(args: Array<String>) {
    val a = "str1"
    val b = "str2"
    val c = "str3"
    //Breakpoint!
    val d = 1
}

// DEBUG_LABEL: a = a
// DEBUG_LABEL: b = b
// DEBUG_LABEL: c = c

// EXPRESSION: a_DebugLabel
// RESULT: "str1": Ljava/lang/String;

// EXPRESSION: b_DebugLabel
// RESULT: "str2": Ljava/lang/String;

// EXPRESSION: c_DebugLabel
// RESULT: "str3": Ljava/lang/String;