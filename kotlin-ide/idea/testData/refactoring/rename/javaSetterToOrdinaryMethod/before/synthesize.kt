fun synthesize(p: SyntheticProperty) {
    val v1 = p.syntheticA
    p.syntheticA = 1
    p.syntheticA += 2
    p.syntheticA++
    val x = p.syntheticA++
    val y = ++p.syntheticA
}