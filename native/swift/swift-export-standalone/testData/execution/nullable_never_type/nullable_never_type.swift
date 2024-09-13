import NullableNeverType

func nullable_never_type() throws {
    meaningOfLife(input: 1)
}

class Nullable_never_typeTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "nullable_never_type", method: withAutorelease(nullable_never_type)),
        ]
    }
}