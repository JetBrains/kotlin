data class Foo(val foo1: Int, val foo2: Int)
data class Bar(val bar1: String, val bar2: String, val bar3: String)

fun foo() = Foo(1, 2)
fun bar() = Bar("a", "b", "c")

fun main() {
    val (f1, f2) = foo()
    val (b1, b2, b3) = bar()
    print(f1 + f2)
    print(b1 + b2 + b3)

    val l1 = listOf(Foo(1, 1))
    for ((x1, x2) in l1) { print(x1 + x2) }
    val l2 = listOf(Bar("a", "a", "a"))
    <warning descr="SSR">for ((x1, x2, x3) in l2) { print(x1 + x2 + x3) }</warning>

    for (i in 1..2) { print(i) }
}