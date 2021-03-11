fun synthesize(p: SyntheticProperty) {
    val v1 = p.foo()
    p.setSyntheticA(1)
    p.setSyntheticA(p.foo() + 2)
    p.setSyntheticA(p.foo().inc())
    val syntheticA = p.foo()
    p.setSyntheticA(syntheticA.inc())
    val x = syntheticA
    val i = p.foo().inc()
    p.setSyntheticA(i)
    val y = i
}