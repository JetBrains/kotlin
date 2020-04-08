package test

enum class E {
    A;

    companion object {
        public const val /*rename*/B : Int = 1
    }
}

fun main(args: Array<String>) {
    val a = E.B + 2
}