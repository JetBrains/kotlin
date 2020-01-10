package parameterNames

fun topLevel(a: Int, b: String) {}

var topLevel: Int
    get() = 0
    set(newValueParam) {}

fun String.ext(other: Int) {}
fun String.ext2(`$this$ext2`: Int) {}

class ExtContainer {
    fun String.ext(other: Int) {}
    fun String.ext2(`$this$ext2`: Int) {}
}

suspend fun main() {}

class Foo {
    fun foo(a: Int, b: String) {}
    suspend fun fooSuspend(a: Int, b: String) {}

    var prop: Int
        get() = 0
        set(newValueParam) {}

    object NestedObj {
        fun bar(a: Int, b: String) {}
    }

    interface NestedIntf {
        fun bar(c: Int, d: String) {}
    }

    inner class InnerCl(val e: Int, f: String)
}

interface Intf {
    fun foo(g: Int, h: String) {}
    suspend fun fooSuspend(i: Int, j: String) {}

    fun foo2(`$this`: Int, h: String) {}
    suspend fun fooSuspend2(`$this`: Int, j: String) {}
}

enum class Enm {
    FOO(1, ""), BAR(2, "");

    fun foo(l: Int, m: String) {}

    constructor(n: Int, s: String) {}
}

annotation class Anno(val t: Int, val o: String) {}

data class User(val firstName: String, val lastName: String, val age: Int)

class Bar(val x: Int) {
    constructor(y: Long) : this(0)
}