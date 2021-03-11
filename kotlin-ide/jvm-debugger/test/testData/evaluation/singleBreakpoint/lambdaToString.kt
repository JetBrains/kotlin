fun main() {
    //Breakpoint!
    val a = 5
}

private fun String.removeId(): String {
    return substringBeforeLast('#') + "#id"
}

// EXPRESSION: {}.toString().removeId()
// RESULT: "kotlin.jvm.internal.Lambda#id": Ljava/lang/String;

// EXPRESSION: { 1 + 2 }.toString().removeId()
// RESULT: "kotlin.jvm.internal.Lambda#id": Ljava/lang/String;

// EXPRESSION: { s: String -> s.toString() }.toString().removeId()
// RESULT: "kotlin.jvm.internal.Lambda#id": Ljava/lang/String;

// EXPRESSION: { l: List<String> -> l.first() }.toString().removeId()
// RESULT: "kotlin.jvm.internal.Lambda#id": Ljava/lang/String;