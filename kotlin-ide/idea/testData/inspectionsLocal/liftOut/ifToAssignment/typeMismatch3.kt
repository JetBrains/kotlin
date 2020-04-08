// PROBLEM: none
class C {
    operator fun plusAssign(i: Int) {}
    operator fun plusAssign(b: Boolean) {}
}

fun f(b: Boolean) {
    val c = C()
    <caret>if (b)
        c += 1
    else
        c += true
}