class B {
    val c = "ab"
}

fun main(args: Array<String>) {
    val a: B? = B()
    val x = a?.<caret>c
}

