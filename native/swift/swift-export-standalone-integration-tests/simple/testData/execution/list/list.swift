import KotlinRuntime
import ListExport
import Testing

func assertReversed<T: Equatable>(reversed: [T], original: [T]) throws {
    try #require(reversed.count == original.count)
    try #require(reversed == original.reversed())
}

let array = [4, 8, 5, 16, 23, 42]

@Test
func testArrayOfInt() throws {
    let original = array.map { Int32($0) }
    try assertReversed(reversed: reverseListInt(l: original), original: original)
}

@Test
func testArrayOfShort() throws {
    let original = array.map { Int16($0) }
    try assertReversed(reversed: reverseListShort(l: original), original: original)
}

@Test
func testArrayOfChar() throws {
    let original = Array("AB0-Ыß☺🙂系".utf16)
    try assertReversed(reversed: reverseListChar(l: original), original: original)
}

@Test
func testArrayOfString() throws {
    let original = array.map { String($0) }
    try assertReversed(reversed: reverseListString(l: original), original: original)
}

@Test
func testArrayOfBox() throws {
    let original = array.map { Box(x: Int32($0)) }
    try assertReversed(reversed: reverseListBox(l: original), original: original)
}

@Test
func testArrayOfOptInt() throws {
    let original = array.map { Int32($0) } + [ nil ]
    try assertReversed(reversed: reverseListOptInt(l: original), original: original)
}

@Test
func testArrayOfOptString() throws {
    let original = array.map { String($0) } + [ nil ]
    try assertReversed(reversed: reverseListOptString(l: original), original: original)
}

@Test
func testArrayOfOptBox() throws {
    let original = array.map { Box(x: Int32($0)) } + [ nil ]
    try assertReversed(reversed: reverseListOptBox(l: original), original: original)
}

@Test
func testArrayOfArray() throws {
    let original = array.map { [Int32($0)] }
    try assertReversed(reversed: reverseListListInt(l: original), original: original)
}

@Test
func testArrayOfOptArray() throws {
    let original = array.map { [Int32($0)] } + [ nil ]
    try assertReversed(reversed: reverseListOptListInt(l: original), original: original)
}

@Test
func testOptArray() throws {
    let originalNil: [Int32]? = .none
    try #require(reverseOptListInt(l: originalNil) == originalNil)

    let originalSome: [Int32]? = array.map { Int32($0) }
    try assertReversed(reversed: reverseOptListInt(l: originalSome)!, original: originalSome!)
}

@Test
func testArrayOfNever() throws {
    let neverArray: [Never] = []
    try #require(reverseListNothing(l: neverArray).isEmpty)

    let optNeverArray: [Never?] = [.none, .none, .none]
    try assertReversed(reversed: reverseListOptNothing(l: optNeverArray), original: optNeverArray)
}

@Test
func testExtArrayOrInt() throws {
    let original = array.map { Int32($0) }
    try assertReversed(reversed: extReverseListInt(original), original: original)
}

@Test
func testExtArrayOrIntProp() throws {
    let original = array.map { Int32($0) }
    try assertReversed(reversed: getExtReverseListIntProp(original), original: original)
}

