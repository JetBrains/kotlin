import LibraryA
import LibraryB

func test() throws {
    let a = MyLibraryA()
    try assertTrue(a.returnMe() == a)
}

class UseLibrariesABTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "test", method: withAutorelease(test)),
        ]
    }
}