fun foo() {
    MainLoop@
    for (i in 1..10) {
        val x = take()
        if (x == null) {
            while (true) {
                break@MainLoop
            }
        }
        <caret>x.hashCode()
    }
}

fun take(): Any? = null
