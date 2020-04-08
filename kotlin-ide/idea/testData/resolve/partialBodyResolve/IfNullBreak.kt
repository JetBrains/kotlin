fun foo() {
    for (i in 1..10) {
        val x = take()
        if (x == null) break
        <caret>x.hashCode()
    }
}

fun take(): Any? = null