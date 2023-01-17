// EXPECTED_REACHABLE_NODES: 1358

open class A(var value: Int)

open class B : A {
    constructor(x: Int) : super(x)

    constructor() : this(180)
}

open class C(val q: Int) : B(q * q) {
    constructor() : this(11)
}

class D : C {
    constructor() : super()
}

class E(e: Int) : C(e)

fun box(): String {
    val ap = A(0) as A

    val bs1 = B(12) as B
    bs1 as A
    val bs2 = B() as B
    bs2 as A

    val cp = C(14) as C
    cp as B
    cp as A
    val cs = C() as C
    cs as B
    cs as A

    val ds = D() as D
    ds as C
    ds as B
    ds as A
    val ep = E(56)  as E
    ep as C
    ep as B
    ep as A

    return "OK"
}
