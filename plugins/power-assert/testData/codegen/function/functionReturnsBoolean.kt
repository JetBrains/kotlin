// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
    "test5" to { test5() },
    "test6" to { test6() },
    "test7" to { test7() },
)

fun funWithVararg(vararg a: String): Boolean { return a.size > 1 }

fun test1() {
    assert(funWithVararg("a"))
}

fun (()-> Boolean).funWithExtension(): Boolean { return this() }

fun test2() {
    val list = arrayOf<String>("a", "b", "c")
    assert({ false }.funWithExtension())
}

context(a: Boolean)
fun funWithContext(): Boolean {
    return a
}

fun test3() {
    assert(with(false) { funWithContext() })
}

fun <T> funWithTypeParameter(): T { return false as T }

fun test4() {
    assert(funWithTypeParameter())
}

inline fun funWithInline(a: () -> Boolean): Boolean {
    return a()
}

fun test5() {
    assert(funWithInline { false })
}

fun test6() {
    fun local(): Boolean { return false }
    assert(local())
}

class A {
    override fun toString(): String {
        return "A"
    }
}
fun funWithNotPrimitiveParameter(b: Int, a: A): Boolean { return false }

fun test7() {
    assert(funWithNotPrimitiveParameter(1, A()))
}