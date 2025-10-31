import Kt


private func testNativeEnumValues() throws {
    let ktEnum = MyKotlinEnum.a
    let nsEnum = ktEnum.toNSEnum()

    switch(nsEnum) {
        case .a: try assertEquals(actual: nsEnum, expected: ktEnum.toNSEnum())
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
