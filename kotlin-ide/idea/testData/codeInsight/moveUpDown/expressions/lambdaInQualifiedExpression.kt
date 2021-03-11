// MOVE: down
fun test() {
    <caret>val foo = foo()
    Bar().also {
        if (foo != null) {
        }
    }
}

fun foo(): String? = null

class Bar