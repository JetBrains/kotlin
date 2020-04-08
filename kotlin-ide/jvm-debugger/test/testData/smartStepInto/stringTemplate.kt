fun foo() {
    <caret>f2("aaa${f1()}")
}

fun f1() = "1"
fun f2(s: String) {}

// EXISTS: f1(), f2(String)