// KJS_WITH_FULL_RUNTIME

class L<T>

class A {
    fun foo(a: L<Int>) = "Int"
    fun foo(a: L<String>) = "String"
    fun L<Int>.bar() = "Int2"
    fun L<String>.bar() = "String2"
}

fun foo(a: L<Int>) = "Int"
fun foo(a: L<String>) = "String"

private class TestClass {
    private val data = mutableListOf<List<Any>>()
    fun withData(data: List<List<Any>>) = apply { this.data.addAll(data) }
    fun withData(row: List<Any>) = apply {
        data.add(row)
    }

    fun getCols(): Int {
        return data.firstOrNull()?.size ?: return 0
    }
}

object B {
    fun baz(vararg v: B) = "[A]"

    fun baz(vararg v: String) = "[S]"
}

class C<in T> {
    fun bac(c: T): String {
        return "T4"
    }

    fun bac(c: Int): String {
        return "Int5"
    }

    fun bac(c: List<T>): String {
        return "ListT4"
    }

    fun bac(c: List<Int>): String {
        return "ListInt4"
    }

    fun bac(c: List<*>): String {
        return "ListStar4"
    }
}

fun box(): String {
    if (A().foo(L<Int>()) != "Int") return "fail1"
    A().apply {
        if (L<Int>().bar() != "Int2") return "fail2"
    }
    if (foo(L<Int>()) != "Int") return "fail3"

    val b = TestClass()
    val data = mutableListOf<List<Any>>()
    data.add(listOf("a", "b", "c"))
    data.add(listOf("d", "e", "f"))
    b.withData(data)
    if (b.getCols() != 3) return "fail4"

    if (B.baz(B) != "[A]") return "fail5"
    if (B.baz("a") != "[S]") return "fail6"

    if(C<String>().bac("a") != "T4") return "fail7"
    if(C<String>().bac(5) != "Int5") return "fail8"
    if(C<String>().bac(listOf("a", "b")) != "ListT4") return "fail9"
    if(C<String>().bac(listOf(5, 6)) != "ListInt4") return "fail10"
    if(C<String>().bac(listOf(Any(), Any())) != "ListStar4") return "fail11"

    return "OK"
}