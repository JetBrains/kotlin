fun foo(c: Collection<String>) {
    for (i in 1..10) {
        val x = take()
        if (x == null) {
            for (s in c) {
                print(s)
                return
            }
        }
        <caret>x.hashCode()
    }
}

fun take(): Any? = null
