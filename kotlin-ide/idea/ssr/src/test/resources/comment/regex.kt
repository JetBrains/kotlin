val foo1 = 1
val foo2 = 2

fun main() {
    <warning descr="SSR">/* bar1 = foo1 */</warning>
    val bar1 = foo1
    // bar2 = 0
    val bar2 = 0
    <warning descr="SSR">// bar3 = foo2</warning>
    val bar3 = foo2
    // bar = foo1
    val bar = foo1

    print(bar1 + bar2 + bar3 + bar)
}