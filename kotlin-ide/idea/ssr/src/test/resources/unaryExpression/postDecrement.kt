fun postDecrement(a: Int): Int {
    var b = a
    <warning descr="SSR">b--</warning>
    <warning descr="SSR">b.dec()</warning>
    return b
}