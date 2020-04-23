fun main() {
    <warning descr="SSR">val foo1: Int = 1</warning>
    <warning descr="SSR">val foo2 = 1</warning>
    val bar = "2"
    print(foo1 + foo2)
    print(bar)
}