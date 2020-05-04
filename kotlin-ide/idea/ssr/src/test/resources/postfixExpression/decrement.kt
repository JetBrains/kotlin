fun postDecrement(a: Int): Int {
    var b = a
    <warning descr="SSR">b--</warning>
    return b
}