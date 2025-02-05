import KotlinRuntime
import SetExport

let data = [4, 8, 5, 16, 23, 42]

func testSetOfInt() throws {
    let original = Set(data.map { Int32($0) })
    try assertEquals(actual: testSetInt(s: original), expected: original)
}

func testSetOfShort() throws {
    let original = Set(data.map { Int16($0) })
    try assertEquals(actual: testSetShort(s: original), expected: original)
}

func testSetOfString() throws {
    let original = Set(data.map { String($0) })
    try assertEquals(actual: testSetString(s: original), expected: original)
}

func testSetOfBox() throws {
    let original = Set(data.map { Box(x: Int32($0)) })
    try assertEquals(actual: testSetBox(s: original), expected: original)
}

func testSetOfOptInt() throws {
    let original = Set(data.map { Int32($0) } + [ nil ])
    try assertEquals(actual: testSetOptInt(s: original), expected: original)
}

func testSetOfOptString() throws {
    let original = Set(data.map { String($0) } + [ nil ])
    try assertEquals(actual: testSetOptString(s: original), expected: original)
}

func testSetOfOptBox() throws {
    let original = Set(data.map { Box(x: Int32($0)) } + [ nil ])
    try assertEquals(actual: testSetOptBox(s: original), expected: original)
}

func testSetOfArray() throws {
    let original = Set(data.map { [Int32($0)] })
    try assertEquals(actual: testSetListInt(s: original), expected: original)
}

func testSetOfSet() throws {
    let original = Set(data.map { Set([Int32($0)]) })
    try assertEquals(actual: testSetSetInt(s: original), expected: original)
}

func testSetOfOptSet() throws {
    let original = Set(data.map { Set([Int32($0)]) } + [ nil ])
    try assertEquals(actual: testSetOptSetInt(s: original), expected: original)
}

func testOptSet() throws {
    let originalNil: Set<Int32>? = nil
    try assertEquals(actual: testOptSetInt(s: originalNil), expected: originalNil)

    let originalSome: Set<Int32>? = Set(data.map { Int32($0) })
    try assertEquals(actual: testOptSetInt(s: originalSome)!, expected: originalSome!)
}

func testSetOfNever() throws {
    let neverSet: Set<Never> = []
    try assertTrue(testSetNothing(s: neverSet).isEmpty)

    let optNeverSet: Set<Never?> = [.none, .none, .none]
    try assertEquals(actual: testSetOptNothing(s: optNeverSet), expected: optNeverSet)
}

class SetTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testSetOfInt", method: withAutorelease(testSetOfInt)),
            TestCase(name: "testSetOfShort", method: withAutorelease(testSetOfShort)),
            TestCase(name: "testSetOfString", method: withAutorelease(testSetOfString)),
            TestCase(name: "testSetOfBox", method: withAutorelease(testSetOfBox)),

            TestCase(name: "testSetOfOptInt", method: withAutorelease(testSetOfOptInt)),
            TestCase(name: "testSetOfOptString", method: withAutorelease(testSetOfOptString)),
            TestCase(name: "testSetOfOptBox", method: withAutorelease(testSetOfOptBox)),

            TestCase(name: "testSetOfArray", method: withAutorelease(testSetOfArray)),
            TestCase(name: "testSetOfSet", method: withAutorelease(testSetOfSet)),
            TestCase(name: "testSetOfOptSet", method: withAutorelease(testSetOfOptSet)),
            TestCase(name: "testOptSet", method: withAutorelease(testOptSet)),
        ]
    }
}