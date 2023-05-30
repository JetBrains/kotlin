fun test(): Int {
    val b = getObjectB()
    val a = getObjectA()
    return b.test() + b.testProp + b.testWithDefault(2) + b.testGeneric(100) +
            a.test() + a.testProp + a.testWithDefault(2) + a.testGeneric(77)
}
