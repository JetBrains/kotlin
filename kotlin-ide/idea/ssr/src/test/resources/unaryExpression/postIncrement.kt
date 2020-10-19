fun postIncrement(a: Int): Int {
    var b = a
    <warning descr="SSR">b++</warning>
    <warning descr="SSR">(b)++</warning>
    <warning descr="SSR">(((b)))++</warning>
    <warning descr="SSR">b.inc()</warning>
    return b
}