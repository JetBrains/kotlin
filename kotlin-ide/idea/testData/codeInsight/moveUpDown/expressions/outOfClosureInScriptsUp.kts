// MOVE: up
fun takeLamb(f: () -> Unit) {}
takeLamb {
    <caret>foo()
}

fun foo() {}