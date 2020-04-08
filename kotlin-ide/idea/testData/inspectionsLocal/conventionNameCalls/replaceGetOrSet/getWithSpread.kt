// PROBLEM: none
class A {
    operator fun get(vararg args: Any) {
        println(args.size)
    }

    private fun println(i: Int) {}
}

fun main() {
    val args = arrayOf("a", 1)
    A().<caret>get(*args)
}
