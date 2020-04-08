// FILE: stopInInlineInOtherFileWithLambdaArgument.kt
package stopInInlineInOtherFileWithLambdaArgument

fun main(args: Array<String>) {
    inlineFun { "hi" }
    val i = 1
}

// ADDITIONAL_BREAKPOINT: stopInInlineInOtherFileWithLambdaArgument.Other.kt: Breakpoint 1

// FILE: stopInInlineInOtherFileWithLambdaArgument.Other.kt
package stopInInlineInOtherFileWithLambdaArgument

inline fun inlineFun(a: () -> Unit) {
    a()
    // Breakpoint 1
    a()
    a()
}