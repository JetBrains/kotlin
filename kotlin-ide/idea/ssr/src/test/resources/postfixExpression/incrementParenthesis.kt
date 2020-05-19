fun postIncrement(a: Int): Int {
    var b = a
    b++
    <warning descr="SSR">(b)++</warning>
    return b
}