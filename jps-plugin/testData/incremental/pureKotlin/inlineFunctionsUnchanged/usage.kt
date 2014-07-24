package usage

fun main(args: Array<String>) {
    inline.f { println("to be inlined") }
    inline.Klass().f { println("to be inlined") }
}
