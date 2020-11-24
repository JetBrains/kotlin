fun foo(x: Any) { print(x) }

fun main() {
    foo(Int::class)
    foo(<warning descr="SSR">Int::class.java</warning>)
}