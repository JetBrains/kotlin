// FLOW: OUT

interface I {
    fun Int.foo(p: Any)
}

class C1 : I {
    override fun Int.foo(p: Any) {
        val v = p // this usage will be shown twice due to bug in Java implementation: https://youtrack.jetbrains.com/issue/IDEA-236958
    }
}

fun I.bar(s: String) {
    1.foo(<caret>s)
}
