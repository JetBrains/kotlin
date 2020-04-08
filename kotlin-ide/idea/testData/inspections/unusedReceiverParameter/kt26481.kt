class My(val x: Int)
class Your(val x: Int)

fun Your.foo(): Int {
    println(x)
    fun My.bar(): Int {
        return x
    }
    return My(0).bar() + x
}

fun bar() {
    fun String.foo() = this.length
    "".foo()
}

fun baz() {
    fun String.foo() = length
    "".foo()
}