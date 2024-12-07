import Kt

private func testFunctionsInDifferentFilesAreNotMangled() throws {
    try assertEquals(actual: TopLevelManglingAKt.foo(), expected: "a1")
    try assertEquals(actual: TopLevelManglingBKt.foo(), expected: "b1")
}

private func testPropertiesInDifferentFilesAreNotMangled() throws {
    try assertEquals(actual: TopLevelManglingAKt.bar, expected: "a2")
    try assertEquals(actual: TopLevelManglingBKt.bar, expected: "b2")
}

private func testFunctionsInSameFileAreMangled() throws {
    try assertEquals(actual: TopLevelManglingAKt.sameNumber(value: Int32(1)), expected: 1)
#if DISABLE_MEMBER_NAME_MANGLING
    let sameNumberLong = TopLevelManglingAKt.sameNumber(value: Int64(2))
#else
    let sameNumberLong = TopLevelManglingAKt.sameNumber(value_: Int64(2))
#endif
    try assertEquals(actual: sameNumberLong, expected: 2)
}

class TopLevelManglingTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestFunctionsInDifferentFilesAreNotMangled", testFunctionsInDifferentFilesAreNotMangled)
        test("TestPropertiesInDifferentFilesAreNotMangled", testPropertiesInDifferentFilesAreNotMangled)
        test("TestFunctionsInSameFileAreMangled", testFunctionsInSameFileAreMangled)
    }
}
