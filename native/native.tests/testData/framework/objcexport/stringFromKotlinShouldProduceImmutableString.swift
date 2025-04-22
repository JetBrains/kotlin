import Kt

private func test1() throws {
    let stringHolder = StringHolder.Companion().con()
    try assertFalse((stringHolder.string as NSString) is NSMutableString)
}

class StringFromKotlinShouldProduceImmutableStringTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}