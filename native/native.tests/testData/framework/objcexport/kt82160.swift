import Kt

private func testInternalInterfaceInheritance() throws {
    let c = C()
    try assertTrue(c is AnyObject)

    let d = D()
    try assertTrue(d is AnyObject)

    let c2 = C2()
    try assertTrue(c2 is AnyObject)

    let d2 = D2()
    try assertTrue(d2 is AnyObject)
}

class Kt82160Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestInternalInterfaceInheritance", testInternalInterfaceInheritance)
    }
}