import Kt


private func testNativeEnumValues() throws {
    let ktEnum = MyKotlinEnum.original
    let nsEnum = ktEnum.nsEnum

    switch(nsEnum) {
        case SwiftFoo.alpha: try fail()
        case .renamed: try assertEquals(actual: nsEnum, expected: ktEnum.nsEnum)
        case .theCopy: try fail()
    }
}

class NativeEnumTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestNativeEnumValues", testNativeEnumValues)
     }
}
