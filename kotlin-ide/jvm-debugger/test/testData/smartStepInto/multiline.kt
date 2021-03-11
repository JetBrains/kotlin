fun foo() {
    <caret>f1(
       f2(),
       f3()
    )
}

fun f1(vararg i: Int) {}
fun f2() = 1
fun f3() = 1

// EXISTS: f1(vararg Int), f2(), f3()