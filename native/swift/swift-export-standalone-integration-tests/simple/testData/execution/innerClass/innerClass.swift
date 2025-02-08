import InnerClass

func testInnerClass() {
    let outer = Outer()
    let inner = Outer.Inner(outer__: outer)
    let innerInner = Outer.Inner.InnerInner(outer__: inner)
}

func testNestedInnerClassWithParameters() throws {
    let outer = OuterWithParam(outerParam: 0)
    let inner = OuterWithParam.InnerWithParam(innerParamA: 1, innerParamB: 2, outer__: outer)
    let innerInner = OuterWithParam.InnerWithParam.InnerInnerWithParam(innerInnerParam: 3, outer__: inner)
    try assertEquals(actual: innerInner.getOuter(), expected: 0)
    try assertEquals(actual: innerInner.getInnerA(), expected: 1)
    try assertEquals(actual: innerInner.getInnerB(), expected: 2)
    try assertEquals(actual: innerInner.getInnerInner(), expected: 3)
}

class InnerClassTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testInnerClass", method: withAutorelease(testInnerClass)),
            TestCase(name: "testNestedInnerClassWithParameters", method: withAutorelease(testNestedInnerClassWithParameters))
        ]
    }
}