fun tw<caret>ice(p: Int): Int {
    val w = p + 1
    return w
}

fun callShadow() {
    var w = twice(2)
    println(twice(3))
    twice(4)
    println("original $w")
}