import Autoimport

func test() throws {
    let descr = useAVFoundation()
    try assertTrue(descr.count > 0)
}

class AutoimportTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "test", method: withAutorelease(test)),
        ]
    }
}