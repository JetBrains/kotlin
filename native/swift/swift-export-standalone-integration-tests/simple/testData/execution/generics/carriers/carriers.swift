import UnboundGenerics
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

// KT-80260: values travelling through `Any` / an unbound type parameter (`any _KotlinBridgeable` in Swift).
// Kotlin reports what it actually received with `describeAny`.

@Test
func boxPropertyGet() throws {
    #expect(stringBox().value as? String == "boxed")
    #expect(boxNothing().value == nil)  // Box<Nothing>: Nothing as a type argument
}

@Test
func boxPropertySetPassesSwiftValues() throws {
    let b = stringBox()
    b.value = "set"
    #expect(boxDescribe(b: b) == "String:set")
    b.value = nil
    #expect(boxDescribe(b: b) == "null")
    #expect(boxDescribe(b: Box(value: Int32(3))) == "Int:3")
}

// KT-90063: vararg of an unbound type parameter loses unsigned types,
// so UInt8/UInt16/UInt32/UInt64 are excluded from the test.
@Test
func varargT() throws {
    #expect(describeVararg(xs: Int8(1)) == "Byte:1")
    #expect(describeVararg(xs: Int16(2)) == "Short:2")
    #expect(describeVararg(xs: Int32(3)) == "Int:3")
    #expect(describeVararg(xs: Int64(4)) == "Long:4")
    #expect(describeVararg(xs: Float(9.5)) == "Float:9.5")
    #expect(describeVararg(xs: Double(10.25)) == "Double:10.25")
    #expect(describeVararg(xs: true) == "Boolean:true")
    #expect(describeVararg(xs: "s") == "String:s")
    #expect(describeVararg(xs: nil) == "null")
    #expect(describeVararg(xs: [Int32(1), Int32(2)]) == "List[Int:1,Int:2]")
    #expect(describeVararg(xs: [Int32]()) == "List[]")
    #expect(describeVararg(xs: Foo(id: 5)) == "Foo:5")
}

@Test
func swiftClosureReturnsT() throws {
    #expect(produce { "s" } == "String:s")
    #expect(produce { nil } == "null")
}

@Test
func swiftClosureMapsT() throws {
    var received: String?
    let result = mapValue(x: "a") { received = $0 as? String; return "b" }
    #expect(received == "a")
    #expect(result as? String == "b")
}

@Test
func kotlinLambdaReturnsT() throws {
    #expect(constant(x: "c")() as? String == "c")
    #expect(constant(x: nil)() == nil, "nil not preserved")
}

@Test
func kotlinLambdaTakesT() throws {
    let f = describer()
    #expect(f("s") == "String:s")
    #expect(f(nil) == "null")
}

@Test
func closurePropertyOnGenericClass() throws {
    let c = Callback()
    #expect(c.fire(x: "k") == "null")            // property unset: Kotlin sees null
    c.listener = { $0 }
    #expect(c.fire(x: "k") == "String:k")        // setter: Kotlin calls the Swift closure, T in and out
    #expect(c.listener?("s") as? String == "s")  // getter: the closure comes back to Swift and is callable
}

@Test
func kotlinLambdaRoundTripsAsAny() throws {
    let f = try #require(lambdaAsT())
    #expect(invokeAny(f: f) == "String:lambda")
}

@Test
func nonNullReceiverIsAMemberOnConcreteTypes() throws {
    #expect("x".nonNullDescribe() == "String:x")
    #expect(Foo(id: 1).nonNullDescribe() == "Foo:1")
}
