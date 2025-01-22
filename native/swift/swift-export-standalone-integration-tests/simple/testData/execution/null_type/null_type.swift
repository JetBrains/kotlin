import NullType
import Testing

@Test
func null_type() throws {
    let nonoptional: Bar = Bar()
    let optional: Bar? = Bar()

    try #require(foo(a: nonoptional) == "nonoptional")
    try #require(foo(a: optional) == "optional")
    try #require(foo() == nil)

    try #require(nullableBar == nil)
    nullableBar = nonoptional
    try #require(nullableBar == nonoptional)
    nullableBar = nil
    try #require(nullableBar == nil)

    try #require(foo_any(a: nonoptional) == "nonoptional")
    try #require(foo_any(a: optional) == "optional")

    try #require(nullableAny == nil)
    nullableAny = nonoptional
    try #require(nullableAny == nonoptional)
    nullableAny = nil
    try #require(nullableAny == nil)

    let f_withNull = Foo(nullableBar: nil)
    let f_withBar = Foo(nullableBar: nonoptional)

    try #require(f_withNull.nullableBar == nil)
    try #require(f_withBar.nullableBar == nonoptional)

    try #require(p_opt_opt_typealias(input: nonoptional) == nonoptional)
    try #require(p_opt_opt_typealias(input: optional) == optional)
    try #require(p_opt_opt_typealias(input: nil) == nil)

    try #require(p_opt_typealias(input: nonoptional) == nonoptional)
    try #require(p_opt_typealias(input: optional) == optional)
    try #require(p_opt_typealias(input: nil) == nil)

    try #require(optionalInt == nil)
    var int = optionalInt
    int = intIdentity(input: 123)
    optionalInt = int
    try #require(optionalInt == 123)

    optionalInt = 1
    try #require(optionalInt == 1)

    optionalInt = Optional<Int32>.some(2)
    try #require(optionalInt == 2)

    optionalInt = nil
    try #require(optionalInt == nil)

    try #require(doubleIdentity(input: nil) == nil)
    try #require(doubleIdentity(input: 1.1) == 1.1)
}

@Test
func null_strings() throws {
    try #require(optionalString == nil)
    try #require(strIdentity(str: nil) == nil)
    try #require(strIdentity(str: "string") == "string")

    var str = optionalString
    str = strIdentity(str: "123")
    optionalString = str
    try #require(optionalString == "123")

    optionalString = "nonoptional string"
    try #require(optionalString == "nonoptional string")

    optionalString = Optional<String>.some("optional string")
    try #require(optionalString == "optional string")

    optionalString = nil
    try #require(optionalString == nil)

    try #require(extPrint(optionalString) == "<null>")
    try #require(extPrint("string") == "string")

    try #require(getExtPrintProp(optionalString) == "<null>")
    try #require(getExtPrintProp("string") == "string")
}

@Test
func null_never() throws {
    try #require(meaningOfLife(input: 42) == nil)
    try #require(meaningOfLife(input: nil) == "optional nothing received")
    try #require(meaningOfLife == nil)

    try #require(multiple_nothings(arg1: nil, arg2: 1, arg3: nil) == nil)

    meaningOfLife = nil
}
