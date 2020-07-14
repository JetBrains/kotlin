val x = "1"
val y = 1

fun main() {
    print(<warning descr="SSR">x.hashCode()</warning>)
    print(y.hashCode())
    print(String.Companion.hashCode())
}