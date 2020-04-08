package boxReturnValue

inline fun <X: Any> useInline(x: X) {
    // EXPRESSION: x
    // RESULT: instance of java.lang.Integer(id=ID): Ljava/lang/Integer;
    //Breakpoint!
    val a = 1
}

fun main(args: Array<String>) {
    useInline(1)
}