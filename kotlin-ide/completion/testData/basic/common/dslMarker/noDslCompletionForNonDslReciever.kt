// FIR_COMPARISON
@DslMarker
annotation class MyDSL

@MyDSL
class HTML

@MyDSL
fun HTML.foo() { }

fun HTML.test() {
    val x = 1
    x.<caret>
}

// ABSENT: foo