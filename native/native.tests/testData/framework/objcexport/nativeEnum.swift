import Kt


private func testNativeEnumValues() throws {
    let ktEnum = MyKotlinEnum.barFoo
    let nsEnum = ktEnum.nsEnum

    switch(nsEnum) {
        case SwiftFoo.alpha: try fail()
        case .barFoo: try assertEquals(actual: nsEnum, expected: ktEnum.nsEnum)
        case .theCopy: try fail()
    }
}

class NativeEnumTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestNativeEnumValues", testNativeEnumValues)
     }
}
