import Inheritance

func inhertianceIsForbidden() throws {
    class Derived: Base {}

    // This will lead to a crash
    Derived()
}

class InheritanceTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "inhertianceIsForbidden", method: withAutorelease(inhertianceIsForbidden)),
        ]
    }
}