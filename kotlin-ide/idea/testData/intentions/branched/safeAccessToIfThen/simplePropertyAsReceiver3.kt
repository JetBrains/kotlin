class B {
    val c = C()
}

class C {
    val d = "bc"
}

fun main(args: Array<String>) {
    val a: B? = B()
    val x = a?.c?.<caret>d
}
