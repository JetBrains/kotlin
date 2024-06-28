import Kt

// Based on https://youtrack.jetbrains.com/issue/KT-44799.
private func testSAMConversion() throws {
    try assertEquals(actual: FunInterfacesKt.getObject().run(), expected: 1)
    try assertEquals(actual: FunInterfacesKt.getLambda().run(), expected: 2)
}

class FunInterfacesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestSAMConversion", testSAMConversion)
    }
}
