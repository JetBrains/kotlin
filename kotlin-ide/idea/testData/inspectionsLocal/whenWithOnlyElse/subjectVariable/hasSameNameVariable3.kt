fun test() {
    <caret>when (val a = 42) {
        else -> use(a)
    }

    val a = 33
}

fun use(i: Int) {}