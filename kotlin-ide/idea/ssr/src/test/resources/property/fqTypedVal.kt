class Foo {
    <warning descr="SSR">val foo = Int()</warning>
    <warning descr="SSR">val bar : Int</warning>

    init {bar = Int()}

    class Int
}

fun main() {
    val foo2: Int = 1
    print(foo2)
}