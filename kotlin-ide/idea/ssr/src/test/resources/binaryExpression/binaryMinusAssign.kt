operator fun Int.minusAssign(b: Int) { this -= b}

fun b() {
    <warning descr="SSR">1 -= 2</warning>
    <warning descr="SSR">1.minusAssign(2)</warning>
}