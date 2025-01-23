import InnerClass

func testInnerClassInit() throws {
    let outer = Outer()
    let inner = Outer.Inner(outer: outer)
    try assertEquals(actual: outer.outerProperty, expected: inner.getOuterProperty())
}

class InnerClassTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testInnerClassInit", method: withAutorelease(testInnerClassInit))
        ]
    }
}