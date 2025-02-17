import Main

func testInterface() throws {
    let expected = SomeFoo()

    let functionResult = identity(obj: expected)
    try assertSame(actual: functionResult, expected: expected)

    property = expected
    let propertyResult = property
    try assertSame(actual: propertyResult, expected: expected)
}

func testNullableInterface() throws {
    let expected = SomeFoo()

    let nullableFunctionResult = nullableIdentity(value: expected)
    try assertSame(actual: nullableFunctionResult, expected: expected)

    nullableProperty = expected
    let propertyResult = nullableProperty
    try assertSame(actual: propertyResult, expected: expected)
}

func testListOfInterfaces() throws {
    let expected = [SomeFoo()]
    let functionResult = listIdentity(value: expected)
    try assertEquals(actual: functionResult, expected: expected)

    listProperty = expected
    let propertyResult = listProperty
    try assertEquals(actual: propertyResult, expected: expected)
}

func testListOfNullableInterfaces() throws {
    let expected = [Optional(SomeFoo())]
    let functionResult = nullablesListIdentity(value: expected)
    try assertEquals(actual: functionResult, expected: expected)

    nullablesListProperty = expected
    let propertyResult = nullablesListProperty
    try assertEquals(actual: propertyResult, expected: expected)
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
            TestCase(name: "testNullableInterface", method: withAutorelease(testNullableInterface)),
            TestCase(name: "testListOfInterfaces", method: withAutorelease(testListOfInterfaces)),
            TestCase(name: "testListOfNullableInterfaces", method: withAutorelease(testListOfNullableInterfaces)),
            TestCase(name: "testInterfaceMembers", method: withAutorelease(testInterfaceMembers)),
        ]
    }
}
