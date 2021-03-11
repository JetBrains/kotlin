fun foo() {
    for (i in 1..10) {
        val x = take()
        if (x == null) {
            while (f()) {
                print(1)
                return
            }
        }
        <caret>x.hashCode()
    }
}

fun take(): Any? = null
fun f(): Boolean{}
