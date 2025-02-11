import Main

func testInterface() throws {
    let expected = SomeFoo()
    let functionResult = identity(obj: expected)
    try assertSame(actual: functionResult, expected: expected)

    property = expected
    let propertyResult = property
    try assertSame(actual: propertyResult, expected: expected)

    let nullableFunctionResult = nullableIdentity(value: expected)
    try assertSame(actual: nullableFunctionResult, expected: expected)
    nullableProperty = expected
    let nullablePropertyResult = nullableProperty
    try assertSame(actual: nullablePropertyResult, expected: expected)
}

func testInterfaceMembers() throws {
    let instance = SomeFoo()

    let expected = SomeFoo()
    let functionResult = instance.identity(obj: expected)
    try assertSame(actual: functionResult, expected: expected)
    try assertFalse(functionResult === instance, "These should not be same")

    instance.property = expected
    let propertyResult = instance.property
    try assertSame(actual: propertyResult, expected: expected)
    try assertFalse(propertyResult === instance, "These should not be same")
}

class ProtocolsTests: TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testInterface", method: withAutorelease(testInterface)),
            TestCase(name: "testInterfaceMembers", method: withAutorelease(testInterfaceMembers)),
        ]
    }
}
