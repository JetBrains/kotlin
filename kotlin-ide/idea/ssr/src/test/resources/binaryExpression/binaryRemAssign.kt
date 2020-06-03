operator fun Int.remAssign(b: Int) { this %= b}

fun b() {
    <warning descr="SSR">1 %= 2</warning>
    <warning descr="SSR">1.remAssign(2)</warning>
}