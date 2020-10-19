class A {
    <warning descr="SSR">val foo1 = Int()</warning>
    <warning descr="SSR">val foo2 : A.Int = Int()</warning>
    class Int
}

fun main() {
    val bar = "1"
    print(bar)
    <warning descr="SSR">val bar1: Int = 1</warning>
    <warning descr="SSR">val bar2: kotlin.Int = 1</warning>
    print(bar1 + bar2)
}