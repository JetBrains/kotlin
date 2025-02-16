import LibraryA

func test() throws {
    try assertTrue(topLevelProperty == 42)
}

class UseLibraryATests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "test", method: withAutorelease(test)),
        ]
    }
}