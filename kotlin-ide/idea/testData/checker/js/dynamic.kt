fun test(d: dynamic): dynamic {
    val d1: dynamic = ""
    var d2: dynamic = null

    d2.foo(1)
    d2[2]
    d2.get(3)
    d2.get(4)

    d2 = d + d1 + 2
    return d2
}