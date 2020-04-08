fun test() = 42

fun foo() {
    val a<caret> = test()
    val b = when (a) {
        1 -> a
        else -> 24
    }
}