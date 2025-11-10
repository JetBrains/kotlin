import Kt


private func testNativeEnumValues() throws {
    let ktEnum = MyKotlinEnum.alpha
    let nsEnum = ktEnum.nsEnum

    switch(nsEnum) {
        case .alpha: try assertEquals(actual: nsEnum, expected: ktEnum.nsEnum)
        case .barFoo: try fail()
        case .theCopy: try fail()
    }
}

class NativeEnumTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestNativeEnumValues", testNativeEnumValues)
     }
}
