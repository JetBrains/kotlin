import NullType

func null_type() throws {
    let nonoptional: Bar = Bar()
    let optional: Bar? = Bar()

    try assertEquals(actual: foo(a: nonoptional), expected: "nonoptional")
    try assertEquals(actual: foo(a: optional), expected: "optional")
    try assertEquals(actual: foo(), expected: nil)

    try assertEquals(actual: nullableBar, expected: nil)
    nullableBar = nonoptional
    try assertEquals(actual: nullableBar, expected: nonoptional)
    nullableBar = nil
    try assertEquals(actual: nullableBar, expected: nil)

    try assertEquals(actual: foo_any(a: nonoptional), expected: "nonoptional")
    try assertEquals(actual: foo_any(a: optional), expected: "optional")

    try assertEquals(actual: nullableAny, expected: nil)
    nullableAny = nonoptional
    try assertEquals(actual: nullableAny, expected: nonoptional)
    nullableAny = nil
    try assertEquals(actual: nullableAny, expected: nil)

    let f_withNull = Foo(nullableBar: nil)
    let f_withBar = Foo(nullableBar: nonoptional)

    try assertEquals(actual: f_withNull.nullableBar, expected: nil)
    try assertEquals(actual: f_withBar.nullableBar, expected: nonoptional)

    try assertEquals(actual: p_opt_opt_typealias(input: nonoptional), expected: nonoptional)
    try assertEquals(actual: p_opt_opt_typealias(input: optional), expected: optional)
    try assertEquals(actual: p_opt_opt_typealias(input: nil), expected: nil)

    try assertEquals(actual: p_opt_typealias(input: nonoptional), expected: nonoptional)
    try assertEquals(actual: p_opt_typealias(input: optional), expected: optional)
    try assertEquals(actual: p_opt_typealias(input: nil), expected: nil)
}

func null_strings() throws {
    try assertEquals(actual: optionalString, expected: nil)
    try assertEquals(actual: strIdentity(str: nil), expected: nil)
    try assertEquals(actual: strIdentity(str: "string"), expected: "string")

    var str = optionalString
    str = strIdentity(str: "123")
    optionalString = str
    try assertEquals(actual: optionalString, expected: "123")

    optionalString = "nonoptional string"
    try assertEquals(actual: optionalString, expected: "nonoptional string")

    optionalString = Optional<String>.some("optional string")
    try assertEquals(actual: optionalString, expected: "optional string")

    optionalString = nil
    try assertEquals(actual: optionalString, expected: nil)
}

class Null_typeTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "null_type", method: withAutorelease(null_type)),
            TestCase(name: "null_strings", method: withAutorelease(null_strings)),
        ]
    }
}