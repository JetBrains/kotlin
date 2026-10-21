import UnboundGenerics
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

// KT-80260: values travelling through `Any` / an unbound type parameter (`any _KotlinBridgeable` in Swift).
// Kotlin reports what it actually received with `describeAny`.

@Test
func stringAsKotlinString() throws {
    #expect(describeT(x: "s") == "String:s")
}

@Test
func int8AsKotlinByte() throws {
    #expect(describeT(x: Int8(1)) == "Byte:1")
}

@Test
func int16AsKotlinShort() throws {
    #expect(describeT(x: Int16(2)) == "Short:2")
}

@Test
func int32AsKotlinInt() throws {
    #expect(describeT(x: Int32(3)) == "Int:3")
}

@Test
func int64AsKotlinLong() throws {
    #expect(describeT(x: Int64(4)) == "Long:4")
}

@Test
func uint8AsKotlinUByte() throws {
    #expect(describeT(x: UInt8(5)) == "UByte:5")
}

@Test
func uint16AsKotlinUShort() throws {
    #expect(describeT(x: UInt16(6)) == "UShort:6")
}

@Test
func uint32AsKotlinUInt() throws {
    #expect(describeT(x: UInt32(7)) == "UInt:7")
}

@Test
func uint64AsKotlinULong() throws {
    #expect(describeT(x: UInt64(8)) == "ULong:8")
}

@Test
func floatAsKotlinFloat() throws {
    #expect(describeT(x: Float(9.5)) == "Float:9.5")
}

@Test
func doubleAsKotlinDouble() throws {
    #expect(describeT(x: 10.25) == "Double:10.25")
}

@Test
func boolAsKotlinBoolean() throws {
    #expect(describeT(x: true) == "Boolean:true")
}

@Test
func nilAsKotlinNull() throws {
    #expect(describeT(x: nil) == "null")
}

@Test
func arrayOfSwiftStructDoesNotCrash() throws {
    #expect(describeAny(x: [MySwiftStruct(), MySwiftStruct()]) == "List[Other,Other]")
}

@Test
func arrayOfKotlinObjectsKeepsObjects() throws {
    let xs = [Foo(id: 5)]
    #expect(firstIsFoo(x: xs))
    #expect(firstFooId(x: xs) == 5)
}

@Test
func arrayOfSwiftIntElements() throws {
    let xs: [Int] = [1, 2]
    #expect(describeAny(x: xs) == "List[Long:1,Long:2]")
}

@Test
func nestedArray() throws {
    #expect(describeAny(x: [[Int32(1), Int32(2)]]) == "List[List[Int:1,Int:2]]")
}

@Test
func arrayWithNil() throws {
    let xs: [String?] = ["a", nil]
    #expect(describeAny(x: xs) == "List[String:a,null]")
}

@Test
func emptyCollections() throws {
    #expect(describeAny(x: [Int32]()) == "List[]")
    #expect(describeAny(x: Set<String>()) == "Set[]")
    #expect(describeAny(x: [String: Int32]()) == "Map[]")
}

@Test
func enumThroughT() throws {
    #expect(describeT(x: Letter.B) == "Enum:B")
}

@Test
func listParamT() throws {
    #expect(describeList(items: [Int32(3)]) == "Int:3")
}

@Test
func emptyBothWays() throws {
    #expect(anyEmpty() as? String == "")
    #expect(isEmpty(x: ""))
}

// Swift-side subclasses and implementations used by the tests above.

private struct MySwiftStruct { let v = 1 }
