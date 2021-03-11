// PROBLEM: none
class B {
    operator fun get(i: Int, vararg args: String) {
        println(args.size)
    }

    private fun println(i: Int) {}
}

fun main() {
    val args = arrayOf("a", "b")
    B().<caret>get(1, *args)
}
