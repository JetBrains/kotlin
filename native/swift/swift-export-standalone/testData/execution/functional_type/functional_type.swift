import FunctionalType

func testCallingClosureReceivedFromKotlin() throws {
    let block = produceClosureIncrementingI()
    try assertEquals(actual: read(), expected: 0)
    block()
    try assertEquals(actual: read(), expected: 1)
    block()
    try assertEquals(actual: read(), expected: 2)
}

class Functional_typeTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testCallingClosureReceivedFromKotlin", method: withAutorelease(testCallingClosureReceivedFromKotlin)),
        ]
    }
}
