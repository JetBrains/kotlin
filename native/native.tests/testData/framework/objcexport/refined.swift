import Kt

extension RefinedKt {
    static func foo() -> Int {
        return Int(RefinedKt.__fooRefined())! * 2
    }

    static func myFoo() -> Int {
        return Int(RefinedKt.__myFooRefined())! * 2
    }

    static var bar: Int {
        return Int(RefinedKt.__barRefined)! * 2
    }

    static var myBar: Int {
        return Int(RefinedKt.__myBarRefined)! * 2
    }
}

extension RefinedClassA {
    func foo() -> Int {
        return Int(__fooRefined())! * 2
    }
}

private func testSwiftRefinements() throws {
    try assertEquals(actual: RefinedKt.foo(), expected: 2)
    try assertEquals(actual: RefinedKt.bar, expected: 6)
}

private func testMySwiftRefinements() throws {
    try assertEquals(actual: RefinedKt.myFoo(), expected: 4)
    try assertEquals(actual: RefinedKt.myBar, expected: 8)
}

private func testInheritedRefinements() throws {
    try assertEquals(actual: RefinedClassA().foo(), expected: 2)
    try assertEquals(actual: RefinedClassB().foo(), expected: 44)
}

class RefinedTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestSwiftRefinements", testSwiftRefinements)
        test("TestMySwiftRefinements", testMySwiftRefinements)
        test("TestInheritedRefinements", testInheritedRefinements)
    }
}
