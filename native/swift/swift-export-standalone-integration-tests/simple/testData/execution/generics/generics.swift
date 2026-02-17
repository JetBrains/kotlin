import Generics
import Testing
import KotlinRuntime

@Test
func smoke() throws {
    let foo = Foo(i: 5)
    try #require(foo == id(param: foo) as! KotlinBase)
    try #require(nil == id(param: nil))
}

@Test
func tripleBox() throws {
    let tb = TripleBox(i: 5)
    let db = tb.t as! Box
    try #require((((tb.t as! Box).t as! Box).t as! Foo).i == 5)
    try #require(((db.t as! Box).t as! Foo).i == 5)
    try #require(tb.unwrap() == 5)

    tb.set(newValue: 3)
    try #require(((db.t as! Box).t as! Foo).i == 3)
}

@Test
func primitiveBox() throws {
    let ib = IntBox(t: 5)
    try #require(ib.t == 5)
    ib.t = 3
    try #require(ib.t == 3)
}

// Verify bridgeable types can be cast back after passing through generic parameters
// or returning from functions with `Any` return type.

@Test
func castStringToString() throws {
    let result = castToString(x: "hello")
    try #require(result == "hello")
}

@Test
func castIntToStringReturnsNil() throws {
    let result = castToString(x: Int32(42))
    try #require(result == nil)
}

@Test
func castIntToInt() throws {
    let result = castToInt(x: Int32(42))
    try #require(result == 42)
}

@Test
func castStringToIntReturnsNil() throws {
    let result = castToInt(x: "hello")
    try #require(result == nil)
}

@Test
func stringThroughGenericToString() throws {
    let result = genericToString(x: "hello")
    try #require(result == "hello")
}

@Test
func intThroughGenericToString() throws {
    let result = genericToString(x: Int32(42))
    try #require(result == "42")
}

@Test
func boolThroughGenericToString() throws {
    let result = genericToString(x: true)
    try #require(result == "true")
}

@Test
func isNullWithString() throws {
    try #require(!isNull(x: "hello"))
}

@Test
func isNullWithNil() throws {
    try #require(isNull(x: nil))
}

@Test
func stringThroughId() throws {
    let result = id(param: "hello") as! String
    try #require(result == "hello")
}

@Test
func int32ThroughId() throws {
    let result = id(param: Int32(42)) as! Int32
    try #require(result == 42)
}

@Test
func arrayPassedThroughIdIsNotNil() throws {
    let result = id(param: [Int32(1), Int32(2), Int32(3)])
    try #require(result != nil)
}

@Test
func anyStringCast() throws {
    let result = anyString() as! String
    try #require(result == "hello")
}

@Test
func anyIntCast() throws {
    let result = anyInt() as! Int32
    try #require(result == 42)
}

@Test
func anyBoolCast() throws {
    let result = anyBool() as! Bool
    try #require(result == true)
}


