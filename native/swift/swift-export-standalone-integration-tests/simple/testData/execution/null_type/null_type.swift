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

    try assertEquals(actual: optionalInt, expected: nil)
    var int = optionalInt
    int = intIdentity(input: 123)
    optionalInt = int
    try assertEquals(actual: optionalInt, expected: 123)

    optionalInt = 1
    try assertEquals(actual: optionalInt, expected: 1)

    optionalInt = Optional<Int32>.some(2)
    try assertEquals(actual: optionalInt, expected: 2)

    optionalInt = nil
    try assertEquals(actual: optionalInt, expected: nil)

    try assertEquals(actual: doubleIdentity(input: nil), expected: nil)
    try assertEquals(actual: doubleIdentity(input: 1.1), expected: 1.1)
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

    try assertEquals(actual: extPrint(optionalString), expected: "<null>")
    try assertEquals(actual: extPrint("string"), expected: "string")

    try assertEquals(actual: getExtPrintProp(optionalString), expected: "<null>")
    try assertEquals(actual: getExtPrintProp("string"), expected: "string")
}

func null_never() throws {
    try assertEquals(actual: meaningOfLife(input: 42), expected: nil)
    try assertEquals(actual: meaningOfLife(input: nil), expected: "optional nothing received")
    try assertEquals(actual: meaningOfLife, expected: nil)

    try assertEquals(actual: multiple_nothings(arg1: nil, arg2: 1, arg3: nil), expected: nil)

    meaningOfLife = nil
}

class Null_typeTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "null_never", method: withAutorelease(null_never)),
            TestCase(name: "null_type", method: withAutorelease(null_type)),
            TestCase(name: "null_strings", method: withAutorelease(null_strings)),
        ]
    }
}
