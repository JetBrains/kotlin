import Char
import Testing

let string = "AB0-Ыß☺🙂系"
func utf16CodeUnit(_ literal: Unicode.Scalar) -> UTF16.CodeUnit {
    return UTF16.CodeUnit(literal.value)
}

@Test
func testKotlinCharToUtf16CodeUnit() throws {
    try #require(getCharAt(index: 4) == utf16CodeUnit("Ы"))
    try #require(getCharAt(index: 5) != utf16CodeUnit("Ы"))

    try string.utf16.enumerated().forEach { i, c in
        try #require(getCharAt(index: numericCast(i)) == c)
    }
}

@Test
func testUtf16CodeUnitToKotlinChar() throws {
    try #require(isEqualToCharAt(c: utf16CodeUnit("ß"), index: 5))
    try #require(!isEqualToCharAt(c: utf16CodeUnit("ß"), index: 4))
    try #require(isEqualToCharAt(c: 0xD83D, index: 7))

    try string.utf16.enumerated().forEach { i, c in
        try #require(isEqualToCharAt(c: c, index: numericCast(i)))
    }
}

@Test
func testNullableChar() throws {
    try #require(charAtIndexOrNull(str: "qwe", index: 0) == utf16CodeUnit("q"))
    try #require(charAtIndexOrNull(str: "q", index: 1) == nil)

    try #require(optionalChar == nil)
    optionalChar = charAtIndexOrNull(str: "qwe", index: 0)
    try #require(optionalChar == utf16CodeUnit("q"))
    optionalChar = charAtIndexOrNull(str: "q", index: 1)
    try #require(optionalChar == nil)
}