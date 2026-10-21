import UnboundGenerics
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

// KT-80260: values travelling through `Any` / an unbound type parameter (`any _KotlinBridgeable` in Swift).
// Kotlin reports what it actually received with `describeAny`.

@Test
func listsAsSwiftArrays() throws {
    #expect(anyListOfInt() as? [Int32] == [1, 2])
    #expect(anyListOfString() as? [String] == ["a", "b"])
    #expect((anyListOfFoo() as? [Foo])?.map(\.id) == [1, 2])
    #expect(anyNestedList() as? [[Int32]] == [[1, 2], [3]])
    #expect(anyListWithNull() as? [Int32?] == [1, nil, 3])
    #expect(anyEmptyList() as? [Int32] == [])
}

@Test
func setsAsSwiftSets() throws {
    #expect(anySetOfString() as? Set<String> == ["a"])
    #expect(anySetOfInt() as? Set<Int32> == [1, 2])
    #expect((anyEmptySet() as? Set<AnyHashable>)?.isEmpty == true)
}

@Test
func mapsAsSwiftDictionaries() throws {
    #expect((anyMapOfStringToFoo() as? [String: Foo])?["k"]?.id == 3)
    #expect(anyMapOfStringToInt() as? [String: Int32] == ["a": 1, "b": 2])
    #expect(anyMapOfStringToString() as? [String: String] == ["key": "value"])
    #expect(anyMapOfListValues() as? [String: [Int32]] == ["xs": [1, 2]])
    #expect(anyEmptyMap() as? [String: Int32] == [:])
}

@Test
func mapWithNullKey() throws {
    let m = anyMapWithNullKey() as? [String?: Int32]
    #expect(m?.count == 2)
    #expect(m?[nil] == 1)
    #expect(m?["a"] == 2)
}

@Test
func mapWithNullValue() throws {
    let m = anyMapWithNullValue() as? [String: Int32?]
    #expect(m?.count == 1)
    #expect(m?["a"] == .some(nil))
}

@Test
func exportedClass() throws {
    let x = anyFoo()
    #expect((x as? Foo)?.id == 42)
    #expect(describeAny(x: x) == "Foo:42")
}

@Test
func interfaceWithHiddenImplementation() throws {
    let shape = try #require(anyShape() as? any Shape)
    #expect(shape.area() == 9)
}

@Test
func kotlinStringIsSwiftString() throws {
    #expect(anyString() as? String == "hello")
}

@Test
func kotlinIntIsInt32() throws {
    let x = anyInt()
    #expect(x as? Int == nil, "cast to Swift Int succeeded")
    #expect(x as? Int32 == 3)
}

@Test
func kotlinByteIsInt8() throws {
    #expect(anyByte() as? Int8 == 1)
}

@Test
func kotlinShortIsInt16() throws {
    #expect(anyShort() as? Int16 == 2)
}

@Test
func kotlinLongIsInt64() throws {
    #expect(anyLong() as? Int64 == 4)
}

@Test
func kotlinUByteIsUInt8() throws {
    #expect(anyUByte() as? UInt8 == 5)
}

@Test
func kotlinUShortIsUInt16() throws {
    #expect(anyUShort() as? UInt16 == 6)
}

@Test
func kotlinUIntIsUInt32() throws {
    #expect(anyUInt() as? UInt32 == 7)
}

@Test
func kotlinULongIsUInt64() throws {
    #expect(anyULong() as? UInt64 == 8)
}

@Test
func kotlinULongMaxIsUInt64Max() throws {
    #expect(uLongMax() as? UInt64 == UInt64.max)
}

@Test
func kotlinFloatIsFloat() throws {
    #expect(anyFloat() as? Float == 9.5)
}

@Test
func kotlinDoubleIsDouble() throws {
    #expect(anyDouble() as? Double == 10.25)
}

@Test
func kotlinBooleanIsBool() throws {
    #expect(anyBoolean() as? Bool == true)
}

@Test
func kotlinListSubclassBecomesSwiftArray() throws {
    let x = customList()
    #expect(x as? [Int32] == [0, 10])
    #expect(x as? KotlinList == nil, "still a KotlinList")
}
