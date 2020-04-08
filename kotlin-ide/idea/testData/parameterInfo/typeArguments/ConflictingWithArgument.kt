// Possible parameter info, for index, lambda and arguments.
// Test is flaky without the fix. In IDEA there's a blinking without the fix with different popups.

fun foo(a: A) {
    a.subscribe(object : L {
        override fun on() {
            withLambda {
                bar<<caret>>()
            }
        }
    })
}

fun withLambda(a: (Int) -> Unit) {}
fun <T> bar() {}

interface L {
    fun on()
}

class A {
    fun subscribe(listener: L) {}
}

/*
Text: (<highlight>T</highlight>), Disabled: false, Strikeout: false, Green: false
*/