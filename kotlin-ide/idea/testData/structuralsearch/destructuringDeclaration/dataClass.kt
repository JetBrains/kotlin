data class Foo(val foo1: Int, val foo2: Int)
data class Bar(val bar1: String, val bar2: String, val bar3: String)

fun foo() = Foo(1, 2)
fun bar() = Bar("a", "b", "c")

fun main() {
    val (f1, f2) = foo()
    <warning descr="SSR">val (b1, b2, b3) = bar()</warning>
    print(f1 + f2)
    print(b1 + b2 + b3)

    val l = listOf(1, 3, 5)
    for ((l1, l2) in l.withIndex()) { print(l1 + l2) }
}