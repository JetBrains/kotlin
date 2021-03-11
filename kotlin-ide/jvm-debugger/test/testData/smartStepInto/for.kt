fun foo() {
    val a = 1
    <caret>for (a in f1()) {
        f2()
    }
}

fun f1() = 1..2
fun f2() {}

// EXISTS: f1()