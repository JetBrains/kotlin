import Smokes

extension Bool {
    static func ^ (left: Bool, right: Bool) -> Bool {
        return left != right
    }
}

func smoke() throws {
    try assertEquals(actual: fooByte(), expected: -1)
    try assertEquals(actual: fooShort(), expected: -1)
    try assertEquals(actual: fooInt(), expected: -1)
    try assertEquals(actual: fooLong(), expected: -1)

    try assertEquals(actual: org.kotlin.plus(a: 1, b: 2, c: 3), expected: 1 + 2 + 3)

    // we expect strange stuff for plus and minus if UInt8 and UInt16 because kotlin behaves that way:
    // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-u-byte/minus.html
    try assertEquals(actual: fooUByte(), expected: UInt8.min)
    try assertEquals(actual: org.kotlin.minus(a: fooUByte(), b: UInt8(1)), expected: UInt32.max)
    try assertEquals(actual: org.kotlin.plus(a: UInt8.max, b: UInt8(1)), expected: UInt32(256))

    try assertEquals(actual: fooUShort(), expected: UInt16.min)
    try assertEquals(actual: org.kotlin.minus(a: fooUShort(), b: UInt16(1)), expected: UInt32.max)
    try assertEquals(actual: org.kotlin.plus(a: UInt16.max, b: UInt16(1)), expected: UInt32(65536))

    try assertEquals(actual: fooUInt(), expected: UInt32.min)
    try assertEquals(actual: org.kotlin.minus(a: fooUInt(), b: UInt32(1)), expected: UInt32.max)
    try assertEquals(actual: org.kotlin.plus(a: UInt32.max, b: UInt32(1)), expected: UInt32.min)

    try assertEquals(actual: fooULong(), expected: UInt64.min)
    try assertEquals(actual: org.kotlin.minus(a: fooULong(), b: UInt64(1)), expected: UInt64.max)
    try assertEquals(actual: org.kotlin.plus(a: UInt64.max, b: UInt64(1)), expected: UInt64.min)

    try assertEquals(actual: org.kotlin.logicalOr(a: true, b: true), expected: true || true)
    try assertEquals(actual: org.kotlin.logicalOr(a: true, b: false), expected: true || false)
    try assertEquals(actual: org.kotlin.logicalOr(a: false, b: false), expected: false || false)

    try assertEquals(actual: org.kotlin.xor(a: true, b: true), expected: true ^ true)
    try assertEquals(actual: org.kotlin.xor(a: true, b: false), expected: true ^ false)
    try assertEquals(actual: org.kotlin.xor(a: false, b: false), expected: false ^ false)
}

class Smoke0Tests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Smokes", method: withAutorelease(smoke)),
        ]
    }
}