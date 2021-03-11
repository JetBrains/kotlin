fun foo() {
    for (i in 1..10) {
        val x = take()
        if (x == null) {
            print(1)
            if (f()) return
        }
        <caret>x.hashCode()
    }
}

fun take(): Any? = null
fun f(): Boolean{}