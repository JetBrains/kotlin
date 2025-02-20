import Smokes
import Testing

extension Bool {
    static func ^ (left: Bool, right: Bool) -> Bool {
        return left != right
    }
}

@Test
func smoke() throws {
    try #require(fooByte() == -1)
    try #require(fooShort() == -1)
    try #require(fooInt() == -1)
    try #require(fooLong() == -1)

    try #require(org.kotlin.plus(a: 1, b: 2, c: 3) == 1 + 2 + 3)

    // we expect strange stuff for plus and minus if UInt8 and UInt16 because kotlin behaves that way:
    // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-u-byte/minus.html
    try #require(fooUByte() == UInt8.min)
    try #require(org.kotlin.minus(a: fooUByte(), b: UInt8(1)) == UInt32.max)
    try #require(org.kotlin.plus(a: UInt8.max, b: UInt8(1)) == UInt32(256))

    try #require(fooUShort() == UInt16.min)
    try #require(org.kotlin.minus(a: fooUShort(), b: UInt16(1)) == UInt32.max)
    try #require(org.kotlin.plus(a: UInt16.max, b: UInt16(1)) == UInt32(65536))

    try #require(fooUInt() == UInt32.min)
    try #require(org.kotlin.minus(a: fooUInt(), b: UInt32(1)) == UInt32.max)
    try #require(org.kotlin.plus(a: UInt32.max, b: UInt32(1)) == UInt32.min)

    try #require(fooULong() == UInt64.min)
    try #require(org.kotlin.minus(a: fooULong(), b: UInt64(1)) == UInt64.max)
    try #require(org.kotlin.plus(a: UInt64.max, b: UInt64(1)) == UInt64.min)

    try #require(org.kotlin.logicalOr(a: true, b: true) == true || true)
    try #require(org.kotlin.logicalOr(a: true, b: false) == true || false)
    try #require(org.kotlin.logicalOr(a: false, b: false) == false || false)

    try #require(org.kotlin.xor(a: true, b: true) == true ^ true)
    try #require(org.kotlin.xor(a: true, b: false) == true ^ false)
    try #require(org.kotlin.xor(a: false, b: false) == false ^ false)
}

@Test
func SmokesDependencies() throws {
    try #require(dependency_usage() == 5)
}

@Test
func SmokeOverrides() throws {
    try #require(foo(arg: Foo()) == "foo")
    try #require(foo(arg: Bar()) == "bar")
    try #require(foo(arg: 1) == "int")

    try #require(foo(arg1: Foo(), arg2: 1) == "foo_int")
    try #require(foo(arg1: 1, arg2: Foo()) == "int_foo")
}