import Kt

private func test1() throws {
    try assertEquals(actual: Kt39206Kt.myFunc(), expected: 17)
}

private func test2() throws {
    try assertTrue(MoreTrickyChars() as AnyObject is MoreTrickyChars)
}

class Kt39206Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
        test("Test2", test2)
    }
}