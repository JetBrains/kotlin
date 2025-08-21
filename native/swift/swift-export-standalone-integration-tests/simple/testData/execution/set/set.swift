import KotlinRuntime
import SetExport
import Testing

let data = [4, 8, 5, 16, 23, 42]

@Test
func testSetOfInt() throws {
    let original = Set(data.map { Int32($0) })
    try #require(testSetInt(s: original) == original)
}

@Test
func testSetOfShort() throws {
    let original = Set(data.map { Int16($0) })
    try #require(testSetShort(s: original) == original)
}

@Test
func testSetOfString() throws {
    let original = Set(data.map { String($0) })
    try #require(testSetString(s: original) == original)
}

@Test
func testSetOfBox() throws {
    let original = Set(data.map { Box(x: Int32($0)) })
    try #require(testSetBox(s: original) == original)
}

@Test
func testSetOfOptInt() throws {
    let original = Set(data.map { Int32($0) } + [ nil ])
    try #require(testSetOptInt(s: original) == original)
}

@Test
func testSetOfOptString() throws {
    let original = Set(data.map { String($0) } + [ nil ])
    try #require(testSetOptString(s: original) == original)
}

@Test
func testSetOfOptBox() throws {
    let original = Set(data.map { Box(x: Int32($0)) } + [ nil ])
    try #require(testSetOptBox(s: original) == original)
}

@Test
func testSetOfArray() throws {
    let original = Set(data.map { [Int32($0)] })
    try #require(testSetListInt(s: original) == original)
}

@Test
func testSetOfSet() throws {
    let original = Set(data.map { Set([Int32($0)]) })
    try #require(testSetSetInt(s: original) == original)
}

@Test
func testSetOfOptSet() throws {
    let original = Set(data.map { Set([Int32($0)]) } + [ nil ])
    try #require(testSetOptSetInt(s: original) == original)
}

@Test
func testOptSet() throws {
    let originalNil: Set<Int32>? = nil
    try #require(testOptSetInt(s: originalNil) == originalNil)

    let originalSome: Set<Int32>? = Set(data.map { Int32($0) })
    try #require(testOptSetInt(s: originalSome)! == originalSome!)
}

@Test
func testSetOfNever() throws {
    let neverSet: Set<Never> = []
    try #require(testSetNothing(s: neverSet).isEmpty)

    let optNeverSet: Set<Never?> = [.none, .none, .none]
    try #require(testSetOptNothing(s: optNeverSet) == optNeverSet)
}