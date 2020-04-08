class F(a: Int?) {
    val b = a
    val c = if (b !=<caret> null) b else 2
}

fun main(args: Array<String>) {
    F(1).c
}
