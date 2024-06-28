import Foundation
import AbstractInstantiation

func testInstantiate() throws {
    // this is failure test, it shouldn't work
    let base = AbstractBase(y: 5)
}

class AbstractInstantiationTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testInstantiate", method: withAutorelease(testInstantiate))
        ]
    }
}