fun main() {
    val a = 2
    val b = 3

    when (b) {
        a -> println()
        in 1..4 -> println()
    }

    <warning descr="SSR">when (b) {
        a -> println()
        else -> println()
    }</warning>
}