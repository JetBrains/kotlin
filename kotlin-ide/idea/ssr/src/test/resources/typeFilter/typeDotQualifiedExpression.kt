val x = "1"
val y = 1

val String.foo: String
get() = "foo"

fun main() {
    print(<warning descr="SSR">x.foo</warning>)
    print(x.hashCode())
    print(y.hashCode())
    print(String.Companion.hashCode())
}