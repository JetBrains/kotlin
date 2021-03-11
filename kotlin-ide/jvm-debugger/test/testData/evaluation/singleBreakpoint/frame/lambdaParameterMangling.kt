package lambdaParameterMangling

private data class Foo(val a: String, val b: Int)

private fun foo(foo: Foo, block: (Foo) -> Unit) {
    block(foo)
}

fun main() {
    foo(Foo("a", 5)) { (a, b) ->
        //Breakpoint!
        val c = 5
    }
}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME

// EXPRESSION: a.length + b
// RESULT: 6: I