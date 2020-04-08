fun f(p1: String, p2: Int, p3: Any){}

class C(val p1: String) {
    val p3: String = ""

    fun foo(p2: Int) {
        f(<caret>)
    }
}

// EXIST: "p1, p2, p3"
