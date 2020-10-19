fun postIncrement(a: Int): Int {
    var b = a
    ++b
    ++(b)
    <warning descr="SSR">++((b))</warning>
    ++(((b)))
    return b
}