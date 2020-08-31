fun tw<caret>ice(p: Int): Int {
    val w = p + 1
    return w
}

fun callShadow() {
    var w = twice(2)
    w = twice(3)
} 