// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun box(): String {
    return test1() +
            test2() +
            test3() +
            test4() +
            test5() +
            test6() +
            test7()
}

fun funWithVararg(vararg a: String): Boolean { return a.size > 1 }

fun test1() = expectThrowableMessage {
    assert(funWithVararg("a"))
}

fun (()-> Boolean).funWithExtension(): Boolean { return this() }

fun test2() = expectThrowableMessage {
    val list = arrayOf<String>("a", "b", "c")
    assert({ false }.funWithExtension())
}

context(a: Boolean)
fun funWithContext(): Boolean {
    return a
}

fun test3() = expectThrowableMessage {
    assert(with(false) { funWithContext() })
}

fun <T> funWithTypeParameter(): T { return false as T }

fun test4() = expectThrowableMessage {
    assert(funWithTypeParameter())
}

inline fun funWithInline(a: () -> Boolean): Boolean {
    return a()
}

fun test5() = expectThrowableMessage {
    assert(funWithInline { false })
}

fun test6() = expectThrowableMessage {
    fun local(): Boolean { return false }
    assert(local())
}

class A {
    override fun toString(): String {
        return "A"
    }
}
fun funWithNotPrimitiveParameter(b: Int, a: A): Boolean { return false }

fun test7() = expectThrowableMessage {
    assert(funWithNotPrimitiveParameter(1, A()))
}