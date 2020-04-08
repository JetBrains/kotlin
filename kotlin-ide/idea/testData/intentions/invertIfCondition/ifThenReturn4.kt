fun foo(p: Int): Int {
    if (p > 0) {
        val x = p / 2
        <caret>if (x <= 1) return 1
        bar1()
    }
    else {
        bar2()
    }
    return 2
}

fun bar1(){}
fun bar2(){}
