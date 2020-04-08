fun foo() {
    for (i in 1..10) {
        val x = take()
        if (x == null) {
            while (true) {
                do {
                    println()
                } while(f())
                break
            }
        }
        <caret>x.hashCode()
    }
}

fun take(): Any? = null
fun f(): Boolean{}
