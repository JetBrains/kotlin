import KotlinRuntime
import MapExport

func assertInversed<K: Hashable, V: Hashable>(inversed: [V: Set<K>], original: [K: V]) throws {
    let expected = Dictionary(grouping: original, by: { $0.value }).mapValues { Set($0.map { $0.key }) }
    try assertEquals(actual: inversed, expected: expected)
}

let data = [1: 37, 2: 37, 3: 42]

func testDictionaryIntString() throws {
    let i2s = Dictionary(uniqueKeysWithValues: data.map { (Int32($0), String($1)) })
    try assertInversed(inversed: inverseMapIntString(m: i2s), original: i2s)
    let s2i = Dictionary(uniqueKeysWithValues: data.map { (String($0), Int32($1)) })
    try assertInversed(inversed: inverseMapStringInt(m: s2i), original: s2i)
}

func testDictionaryLongBox() throws {
    let l2b = Dictionary(uniqueKeysWithValues: data.map { (Int64($0), Box(x: Int32($1))) })
    try assertInversed(inversed: inverseMapLongBox(m: l2b), original: l2b)
    let b2l = Dictionary(uniqueKeysWithValues: data.map { (Box(x: Int32($0)), Int64($1)) })
    try assertInversed(inversed: inverseMapBoxLong(m: b2l), original: b2l)
}

func testDictionaryOptIntListInt() throws {
    let l2b = Dictionary(uniqueKeysWithValues: data.map { (k, v) in
        let optKey: Int32? = if k == 3 { nil } else { Int32(k) }
        return (optKey, [Int32(v), Int32(108)])
    })
    try assertInversed(inversed: inverseMapOptIntListInt(m: l2b), original: l2b)
}

func testDictionaryNeverNever() throws {
    let dict: [Never: Never] = [:]
    try assertTrue(inverseMapNothingNothing(m: dict).isEmpty)
}

class MapTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testDictionaryIntString", method: withAutorelease(testDictionaryIntString)),
            TestCase(name: "testDictionaryLongBox", method: withAutorelease(testDictionaryLongBox)),
            TestCase(name: "testDictionaryOptIntListInt", method: withAutorelease(testDictionaryOptIntListInt)),
            TestCase(name: "testDictionaryNeverOptNever", method: withAutorelease(testDictionaryNeverNever)),
        ]
    }
}