import Char

let string = "AB0-Ыß☺🙂系"
func utf16CodeUnit(_ literal: Unicode.Scalar) -> UTF16.CodeUnit {
    return UTF16.CodeUnit(literal.value)
}

func testKotlinCharToUtf16CodeUnit() throws {
    try assertTrue(getCharAt(index: 4) == utf16CodeUnit("Ы"))
    try assertFalse(getCharAt(index: 5) == utf16CodeUnit("Ы"))

    try string.utf16.enumerated().forEach { i, c in
        try assertEquals(actual: getCharAt(index: numericCast(i)), expected: c)
    }
}

func testUtf16CodeUnitToKotlinChar() throws {
    try assertTrue(isEqualToCharAt(c: utf16CodeUnit("ß"), index: 5))
    try assertFalse(isEqualToCharAt(c: utf16CodeUnit("ß"), index: 4))
    try assertTrue(isEqualToCharAt(c: 0xD83D, index: 7))

    try string.utf16.enumerated().forEach { i, c in
        try assertTrue(isEqualToCharAt(c: c, index: numericCast(i)))
    }
}

class CharTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testKotlinCharToUtf16CodeUnit", method: withAutorelease(testKotlinCharToUtf16CodeUnit)),
            TestCase(name: "testUtf16CodeUnitToKotlinChar", method: withAutorelease(testUtf16CodeUnitToKotlinChar)),
        ]
    }
}
