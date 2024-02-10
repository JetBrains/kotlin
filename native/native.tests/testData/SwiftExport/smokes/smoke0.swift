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