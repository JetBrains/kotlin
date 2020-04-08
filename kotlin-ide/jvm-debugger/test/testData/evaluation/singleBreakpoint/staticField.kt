package staticField

fun main() {
    x
}

val x: String = "x"
    //FunctionBreakpoint!
    get() = "foo" + field

// EXPRESSION: field
// RESULT: "x": Ljava/lang/String;