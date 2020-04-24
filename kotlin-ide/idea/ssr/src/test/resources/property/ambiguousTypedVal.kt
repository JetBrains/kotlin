class A {
    <warning descr="SSR">val foo1 = Int()</warning>
    class Int
}

fun main() {
    <warning descr="SSR">val foo2: Int = 1</warning>
    print(foo2)
}