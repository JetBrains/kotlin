import Main
import Testing

@Test
func testInterface() throws {
    let expected = SomeFoo()

    let functionResult = identity(obj: expected)
    #expect(functionResult === expected)

    property = expected
    let propertyResult = property
    #expect(propertyResult === expected)
}

func testNullableInterface() throws {
    let expected = SomeFoo()

    let nullableFunctionResult = nullableIdentity(value: expected)
    #expect(nullableFunctionResult === expected)

    nullableProperty = expected
    let propertyResult = nullableProperty
    #expect(propertyResult === expected)
}

func testListOfInterfaces() throws {
    let expected = [SomeFoo()]
    let functionResult = listIdentity(value: expected)
    #expect(functionResult == expected)

    listProperty = expected
    let propertyResult = listProperty
    #expect(propertyResult == expected)
}

func testListOfNullableInterfaces() throws {
    let expected = [Optional(SomeFoo())]
    let functionResult = nullablesListIdentity(value: expected)
    #expect(functionResult == expected)

    nullablesListProperty = expected
    let propertyResult = nullablesListProperty
    #expect(propertyResult == expected)
}

@Test
func testInterfaceMembers() throws {
    let instance = SomeFoo()

    let expected = SomeFoo()
    let functionResult = instance.identity(obj: expected)
    #expect(functionResult === expected)
    #expect(functionResult !== instance, "These should not be same")

    instance.property = expected
    let propertyResult = instance.property
    #expect(propertyResult === expected)
    #expect(propertyResult !== instance, "These should not be same")
}

@Test
func testInterfaceMembersOfExistential() throws {
    let instance: any Foo = SomeFoo()

    let expected = SomeFoo()
    let functionResult = instance.identity(obj: expected)
    try #require(functionResult === expected)
    try #require(functionResult !== instance)

    instance.property = expected
    let propertyResult = instance.property
    try #require(propertyResult === expected)
    try #require(propertyResult !== instance)
}

@Test
func testShouldWrapPrivateTypesIntoKotlinExistentialsInFunctions() throws {
    let expected = value
    #expect(expected is Baz)

    let actualFunctionResult = identity(baz: expected)

    #expect(ObjectIdentifier(actualFunctionResult) == ObjectIdentifier(expected))
    #expect(actualFunctionResult === expected)
}

@Test
func testShouldCallMethodsThroughKotlinExistentials() throws {
    let instance: Baz = value
    let result = instance.identity(baz: instance)
    #expect(ObjectIdentifier(result) == ObjectIdentifier(instance))
    #expect(result === instance)
}

@Test
func testShouldAccessPropertiesThroughKotlinExistentials() throws {
    let instance: Baz = value

    instance.value = instance
    let retrieved = instance.value
    #expect(ObjectIdentifier(retrieved) == ObjectIdentifier(instance))
    #expect(retrieved === instance)
}

@Test
func testShouldWrapPrivateTypesIntoKotlinExistentialsInVariables() throws {
    let original = value
    let newInstance = identity(baz: original)

    value = newInstance
    #expect(value === newInstance)

    value = original
}