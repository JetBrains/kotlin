import KotlinRuntime
import ListExport

func assertReversed<T: Equatable>(reversed: [T], original: [T]) throws {
    try assertEquals(actual: reversed.count, expected: original.count)
    try assertEquals(actual: reversed, expected: original.reversed())
}

let array = [4, 8, 5, 16, 23, 42]

func testArrayOfInt() throws {
    let original = array.map { Int32($0) }
    try assertReversed(reversed: reverseListInt(l: original), original: original)
}

func testArrayOfShort() throws {
    let original = array.map { Int16($0) }
    try assertReversed(reversed: reverseListShort(l: original), original: original)
}

func testArrayOfChar() throws {
    let original = Array("AB0-Ыß☺🙂系".utf16)
    try assertReversed(reversed: reverseListChar(l: original), original: original)
}

func testArrayOfString() throws {
    let original = array.map { String($0) }
    try assertReversed(reversed: reverseListString(l: original), original: original)
}

func testArrayOfBox() throws {
    let original = array.map { Box(x: Int32($0)) }
    try assertReversed(reversed: reverseListBox(l: original), original: original)
}

func testArrayOfOptInt() throws {
    let original = array.map { Int32($0) } + [ nil ]
    try assertReversed(reversed: reverseListOptInt(l: original), original: original)
}

func testArrayOfOptString() throws {
    let original = array.map { String($0) } + [ nil ]
    try assertReversed(reversed: reverseListOptString(l: original), original: original)
}

func testArrayOfOptBox() throws {
    let original = array.map { Box(x: Int32($0)) } + [ nil ]
    try assertReversed(reversed: reverseListOptBox(l: original), original: original)
}

func testArrayOfArray() throws {
    let original = array.map { [Int32($0)] }
    try assertReversed(reversed: reverseListListInt(l: original), original: original)
}

func testArrayOfOptArray() throws {
    let original = array.map { [Int32($0)] } + [ nil ]
    try assertReversed(reversed: reverseListOptListInt(l: original), original: original)
}

func testOptArray() throws {
    let originalNil: [Int32]? = .none
    try assertEquals(actual: reverseOptListInt(l: originalNil), expected: originalNil)

    let originalSome: [Int32]? = array.map { Int32($0) }
    try assertReversed(reversed: reverseOptListInt(l: originalSome)!, original: originalSome!)
}

func testArrayOfNever() throws {
    let neverArray: [Never] = []
    try assertTrue(reverseListNothing(l: neverArray).isEmpty)

    let optNeverArray: [Never?] = [.none, .none, .none]
    try assertReversed(reversed: reverseListOptNothing(l: optNeverArray), original: optNeverArray)
}

func testExtArrayOrInt() throws {
    let original = array.map { Int32($0) }
    try assertReversed(reversed: extReverseListInt(receiver: original), original: original)
}

class ListTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testArrayOfInt", method: withAutorelease(testArrayOfInt)),
            TestCase(name: "testArrayOfShort", method: withAutorelease(testArrayOfShort)),
            TestCase(name: "testArrayOfChar", method: withAutorelease(testArrayOfChar)),
            TestCase(name: "testArrayOfString", method: withAutorelease(testArrayOfString)),
            TestCase(name: "testArrayOfBox", method: withAutorelease(testArrayOfBox)),

            TestCase(name: "testArrayOfOptInt", method: withAutorelease(testArrayOfOptInt)),
            TestCase(name: "testArrayOfOptString", method: withAutorelease(testArrayOfOptString)),
            TestCase(name: "testArrayOfOptBox", method: withAutorelease(testArrayOfOptBox)),

            TestCase(name: "testArrayOfArray", method: withAutorelease(testArrayOfArray)),
            TestCase(name: "testArrayOfOptArray", method: withAutorelease(testArrayOfOptArray)),
            TestCase(name: "testOptArray", method: withAutorelease(testOptArray)),

            TestCase(name: "testExtArrayOrInt", method: withAutorelease(testExtArrayOrInt)),
        ]
    }
}