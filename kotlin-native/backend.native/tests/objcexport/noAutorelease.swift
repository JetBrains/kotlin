import Kt

private func test1() throws {
    let test = TestNoAutorelease()
    try assertEquals(actual: test.returnObj()?.x, expected: 11)
    try assertEquals(actual: test.returnObj()?.x, expected: 11)
    test.clearObj()
    try assertTrue(test.isObjUnreachable())
}

class NoAutoreleaseTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
        test("Test1_2", test1)
    }
}
