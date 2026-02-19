fun B.message() = p1 + p2 + p3 + f1() + f2() + f3()

fun box(stepId: Int, isWasm: Boolean): String {
    val c = C().message()
    val d = D().message()
    when (stepId) {
        7  -> if (c != "HELLO, wORLD!" || d != "HELLO, WORLD!") return "fail on change superclass to interface"
        8  -> if (c != "Hello, world!" || d != "Hello, World!") return "fail on implemented interface default implementation change"
        9  -> if (c != "Hello, world!" || d != "Hello, World!") return "fail on implemented interface declarations order change"
        10 -> if (c != "Hello, world!" || d != "Hello, World!") return "fail on implemented interface add property"
        11 -> if (c != "Hello, world!" || d != "Hello, World!") return "fail on implemented interface add function"
        12 -> if (c != "Hello, world!" || d != "Hello, World!") return "fail on implemented interface remove property"
        13 -> if (c != "Hello, world!" || d != "Hello, World!") return "fail on implemented interface remove function"
    }
    return "OK"
}