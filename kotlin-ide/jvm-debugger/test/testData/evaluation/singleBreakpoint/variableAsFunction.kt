package variableAsFunction

fun main() {
    class Foo

    class Bar {
        operator fun Foo.invoke(f: Int) = f
    }

    with (Bar()) {
        block {
            val foo = Foo()
            //Breakpoint!
            val y = foo(5)
        }
    }
}

fun block(block: () -> Unit) {
    block()
}


// EXPRESSION: foo(5)
// RESULT: 5: I