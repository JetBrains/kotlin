package foo

class A() {
    fun foo() = 1
    fun a(f: A.() -> Int): Int {
        return f()
    }
}

var d = 0

val p: A.() -> Int = {
    d = foo()
    d++
}

val c = A().a(p)

fun box() = (c == 1)
