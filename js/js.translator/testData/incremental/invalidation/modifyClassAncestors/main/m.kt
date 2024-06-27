fun B.message() = p1 + p2 + p3 + f1() + f2() + f3()

fun box(stepId: Int): String {
    val a = A().p2 + A().f2()
    val c = C().message()
    val d = D().message()
    when (stepId) {
        0  -> if (a != "PF" || c != "HELLO, wORLD!" || d != "HELLO, WORLD!") return "fail on initial setup"
        1  -> if (a != "PF" || c != "Hello, world!" || d != "Hello, World!") return "fail on superclass body change"
        2  -> if (a != "PF" || c != "Hello, world!" || d != "Hello, World!") return "fail on superclass declarations order change"
        3  -> if (a != "PF" || c != "Hello, world!" || d != "Hello, World!") return "fail on superclass add property"
        4  -> if (a != "PF" || c != "Hello, world!" || d != "Hello, World!") return "fail on superclass add function"
        5  -> if (a != "PF" || c != "Hello, world!" || d != "Hello, World!") return "fail on superclass remove property"
        6  -> if (a != "PF" || c != "Hello, world!" || d != "Hello, World!") return "fail on superclass remove function"
    }
    return "OK"
}