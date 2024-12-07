fun test(): Int {
    val a = getObjectA()
    val b = getObjectB()
    val c = getObjectC()

    return a.testA1() + a.testA2() +
            b.testA1() + b.testA2() + b.testB1() +
            c.testA1() + c.testA2() + c.testB1() + c.testC1()
}
