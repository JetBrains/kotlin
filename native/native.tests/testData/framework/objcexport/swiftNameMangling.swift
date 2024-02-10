import Kt

private func test1() throws {
    let i1 = SwiftNameManglingKt.i1()
    let i2 = SwiftNameManglingKt.i2()

#if DISABLE_MEMBER_NAME_MANGLING || DISABLE_INTERFACE_METHOD_NAME_MANGLING
    try assertEquals(actual: i1.clashingProperty, expected: 1)
    try assertEquals(actual: i1.clashingMethod(), expected: 2)
    try assertEquals(actual: i1.swiftClashingMethodWithObjCNameInI1(), expected: 3)
    try assertEquals(actual: i1.swiftClashingMethodWithObjCNameInI2(), expected: 4)
    try assertEquals(actual: i1.swiftClashingMethodWithObjCNameInBoth(), expected: 5)

    try assertEquals(actual: i2.clashingProperty as! String, expected: "one")
    try assertEquals(actual: i2.clashingMethod() as! String, expected: "two")
    try assertEquals(actual: i2.swiftClashingMethodWithObjCNameInI1() as! String, expected: "three")
    try assertEquals(actual: i2.swiftClashingMethodWithObjCNameInI2() as! String, expected: "four")
    try assertEquals(actual: i2.swiftClashingMethodWithObjCNameInBoth() as! String, expected: "five")
#endif
}

class SwiftNameManglingTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}