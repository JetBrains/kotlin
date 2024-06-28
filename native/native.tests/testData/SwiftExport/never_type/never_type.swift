import NeverType

func neverType() throws {
    meaningOfLife()
}

class Never_typeTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "NeverType", method: withAutorelease(neverType)),
        ]
    }
}