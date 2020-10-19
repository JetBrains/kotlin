fun Int.a(): Int {
    return inv()
}

fun b(): Int {
    return <warning descr="SSR">0.a()</warning>
}