import Kt

private func test1() throws {
    let actual = ExplicitBlockParameterNamesKt.callMyParametersExplicitlyPlease(cb: { a, b, c in "\(a) \(b) \(c)" })
    try assertEquals(actual: actual, expected: "5 6 hello")
}

class ExplicitBlockParameterNamesTests : SimpleTestProvider {
    override init() {
        super.init()
        test("Test1", test1)
    }
}