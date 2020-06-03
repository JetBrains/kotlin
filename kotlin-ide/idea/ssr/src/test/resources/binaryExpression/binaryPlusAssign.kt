operator fun Int.plusAssign(b: Int) { this += b}

fun b() {
    <warning descr="SSR">1 += 2</warning>
    <warning descr="SSR">1.plusAssign(2)</warning>
}