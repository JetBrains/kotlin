var x = 1
var y = 1.0

fun main() {
    <warning descr="SSR">x++</warning>
    y++
    print(x + y)
}