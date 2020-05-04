fun a() {
    <warning descr="SSR">try {
        println(0)
    } catch (e: Exception) {
        println(1)
    }</warning>

    try {
        println(0)
    } catch (e: Exception) {
        println(2)
    }

    try {
        println(1)
    } catch (e: Exception) {
        println(1)
    }

    try {
        println(0)
    } catch (e: IllegalStateException) {
        println(1)
    }
}