import Main

func testThrowingFunction() throws {
    try assertFailsWith(Error.self) {
        try throwingFunctionThatThrows(value: "error")
    }
}

func testThrowingConstructor() throws {
    try assertFailsWith(Error.self) {
        try NonConstructible(value: "error")
    }
}


class ExceptionsTests: TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testThrowingFunction", method: withAutorelease(testThrowingFunction)),
            TestCase(name: "testThrowingConstructor", method: withAutorelease(testThrowingConstructor)),
        ]
    }
}
