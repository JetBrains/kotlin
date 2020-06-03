operator fun Int.divAssign(b: Int) { this /= b}

fun b() {
    <warning descr="SSR">1 /= 2</warning>
    <warning descr="SSR">1.divAssign(2)</warning>
}