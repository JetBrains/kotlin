operator fun Int.timesAssign(b: Int) { this *= b}

fun b() {
    <warning descr="SSR">1 *= 2</warning>
    <warning descr="SSR">1.timesAssign(2)</warning>
}