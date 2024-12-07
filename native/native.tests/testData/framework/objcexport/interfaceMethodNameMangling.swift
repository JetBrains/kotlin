import Kt

private func test1() throws {
    let i1 = InterfaceMethodNameManglingKt.i1()
    let i2 = InterfaceMethodNameManglingKt.i2()
    let o1 = InterfaceMethodNameManglingKt.o1()
    let o2 = InterfaceMethodNameManglingKt.o2()

#if DISABLE_MEMBER_NAME_MANGLING || DISABLE_INTERFACE_METHOD_NAME_MANGLING
    try assertEquals(actual: i1.clashingProperty, expected: 1)
    try assertEquals(actual: i1.clashingMethod(), expected: 2)
    try assertEquals(actual: i1.interfaceClashingMethodWithObjCNameInI1(), expected: 3)
    try assertEquals(actual: i1.interfaceClashingMethodWithObjCNameInI2(), expected: 4)
    try assertEquals(actual: i1.interfaceClashingMethodWithObjCNameInBoth(), expected: 5)

    try assertEquals(actual: i2.clashingProperty as! String, expected: "one")
    try assertEquals(actual: i2.clashingMethod() as! String, expected: "two")
    try assertEquals(actual: i2.interfaceClashingMethodWithObjCNameInI1() as! String, expected: "three")
    try assertEquals(actual: i2.interfaceClashingMethodWithObjCNameInI2() as! String, expected: "four")
    try assertEquals(actual: i2.interfaceClashingMethodWithObjCNameInBoth() as! String, expected: "five")

    try assertEquals(actual: o1.clashingProperty, expected: "one")
    try assertEquals(actual: o1.clashingMethod(), expected: "two")
#endif
    try assertEquals(actual: o2.clashingProperty, expected: 1)
    try assertEquals(actual: o2.clashingMethod(), expected: 2)
}

class InterfaceMethodNameManglingTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}