fun foo() {
    for (i in 1..10) {
        val x = take()
        if (x == null) {
            while (true) {
                if (g()) break
            }
        }
        <caret>x.hashCode()
    }
}

fun take(): Any? = null
fun f(): Boolean{}
fun g(): Boolean{}