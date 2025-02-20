import KotlinRuntime
import MapExport
import Testing

func assertInversed<K: Hashable, V: Hashable>(inversed: [V: Set<K>], original: [K: V]) throws {
    let expected = Dictionary(grouping: original, by: { $0.value }).mapValues { Set($0.map { $0.key }) }
    try #require(inversed == expected)
}

let data = [1: 37, 2: 37, 3: 42]

@Test
func testDictionaryIntString() throws {
    let i2s = Dictionary(uniqueKeysWithValues: data.map { (Int32($0), String($1)) })
    try assertInversed(inversed: inverseMapIntString(m: i2s), original: i2s)
    let s2i = Dictionary(uniqueKeysWithValues: data.map { (String($0), Int32($1)) })
    try assertInversed(inversed: inverseMapStringInt(m: s2i), original: s2i)
}

@Test
func testDictionaryLongBox() throws {
    let l2b = Dictionary(uniqueKeysWithValues: data.map { (Int64($0), Box(x: Int32($1))) })
    try assertInversed(inversed: inverseMapLongBox(m: l2b), original: l2b)
    let b2l = Dictionary(uniqueKeysWithValues: data.map { (Box(x: Int32($0)), Int64($1)) })
    try assertInversed(inversed: inverseMapBoxLong(m: b2l), original: b2l)
}

@Test
func testDictionaryOptIntListInt() throws {
    let l2b = Dictionary(uniqueKeysWithValues: data.map { (k, v) in
        let optKey: Int32? = if k == 3 { nil } else { Int32(k) }
        return (optKey, [Int32(v), Int32(108)])
    })
    try assertInversed(inversed: inverseMapOptIntListInt(m: l2b), original: l2b)
}

@Test
func testDictionaryNeverNever() throws {
    let dict: [Never: Never] = [:]
    try #require(inverseMapNothingNothing(m: dict).isEmpty)
}