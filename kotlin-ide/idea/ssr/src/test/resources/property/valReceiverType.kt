<warning descr="SSR">val foo: (Int) -> String? = {_ -> null}</warning>

fun main() {
    val bar: String = "bar"
    val bar2: (Int, Int) -> String? = { _, _ -> null }
    print(foo(1))
    print(bar + bar2(0, 0) ?: "")
}