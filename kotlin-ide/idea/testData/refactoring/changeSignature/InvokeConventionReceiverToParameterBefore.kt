class A

operator fun A.<caret>invoke(s: String, i: Int) {}

fun usage(a: A) {
    a("", 42)
    A()("", 42)
    a.invoke("", 42)
}