fun synthesize(p: SyntheticProperty) {
    val v1 = p.syntheticA
    p.foo(1)
    p.foo(p.syntheticA + 2)
    p.foo(p.syntheticA.inc())
    val syntheticA = p.syntheticA
    p.foo(syntheticA.inc())
    val x = syntheticA
    val i = p.syntheticA.inc()
    p.foo(i)
    val y = i
}