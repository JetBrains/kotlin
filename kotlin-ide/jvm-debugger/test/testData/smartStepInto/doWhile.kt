fun foo() {
   <caret>do {
        f2()
    } while (f1())
}

fun f1() = true
fun f2() {}

// EXISTS: f1()