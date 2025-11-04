import Kt


private func testNativeEnumValues() throws {
    let ktEnum = MyKotlinEnum.a
    let nsEnum = ktEnum.nsEnum

    switch(nsEnum) {
        case .a: try assertEquals(actual: nsEnum, expected: ktEnum.nsEnum)
        case .b: try fail()
        case .c: try fail()
    }
}

class NativeEnumTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestNativeEnumValues", testNativeEnumValues)
     }
}
