fun foo() {
    <caret>f1() {
        f2()
    }
}

fun f1(f: () -> Unit) {}
fun f2() {}

// EXISTS: f1(() -> Unit), f1: f.invoke()