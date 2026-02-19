import Properties
import Testing

@Test
func testConstants() throws {
    try #require(BOOLEAN_CONST == true)
    try #require(CHAR_CONST == 65)
    try #require(BYTE_CONST == 1)
    try #require(SHORT_CONST == 2)
    try #require(INT_CONST == 3)
    try #require(LONG_CONST == 4)
    try #require(FLOAT_CONST == 5.0)
    try #require(DOUBLE_CONST == 6.0)
    try #require(UBYTE_CONST == 1)
    try #require(USHORT_CONST == 2)
    try #require(UINT_CONST == 3)
    try #require(ULONG_CONST == 4)
}

@Test
func testLateinit() throws {
    lateinitProperty = Foo(value: 15)
    try #require(compare(a: lateinitProperty, b: Foo(value: 15)))
    lateinitProperty = Foo(value: 42)
    try #require(compare(a: lateinitProperty, b: Foo(value: 42)))
}