import InnerClass

func testSimpleInnerClass() throws {
    let outer = Outer()
    let inner = Outer.Inner(outer: outer)
    try assertEquals(actual: outer.outerProperty, expected: inner.getOuterProperty())

}

func testNestedInnerClass() throws {
    let outer = Outer()
    let inner = Outer.Inner(outer: outer)
    let innerInner = Outer.Inner.InnerInner(outer: inner)
    try assertEquals(actual: outer.outerProperty, expected: innerInner.getOutPropertyFromInnerClass())
}

class InnerClassTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testSimpleInnerClass", method: withAutorelease(testSimpleInnerClass)),
            TestCase(name: "testNestedInnerClass", method: withAutorelease(testNestedInnerClass))
        ]
    }
}