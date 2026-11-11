import UnboundGenerics
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

// KT-80260: values travelling through `Any` / an unbound type parameter (`any _KotlinBridgeable` in Swift).
// Kotlin reports what it actually received with `describeAny`.

@Test
func swiftOverrideReturnsString() throws {
    #expect(consumeOpenProducer(p: SwiftStringProducer()) == "String:hello")
}

@Test
func swiftInterfaceImplementationReturnsString() throws {
    let p = SwiftAnyProducerImpl()
    #expect(consumeAnyProducer(p: p) == "String:from-swift")
    #expect(isString(p: p))
}

@Test
func swiftGenericInterfaceImplementationReturnsInt32() throws {
    #expect(consumeProducer(p: SwiftGenericProducerImpl()) == "Int:5")
}

@Test
func nullableAnyOverride() throws {
    #expect(nullable(s: NullableInt32Source(41)) == 42)
    #expect(nullable(s: NullableInt32Source(nil)) == -1)
}

@Test
func genericTOverrideInt32() throws {
    #expect(genericDouble(s: GenericInt32Source()) == 42)
}

@Test
func kotlinIntIntoSwiftAnyOverrideParam() throws {
    #expect(sendIntToSwift(sink: Int32Sink()) == "Int32:42")
}

@Test
func swiftInConsumerImplReceivesKotlinValues() throws {
    #expect(consumeKotlinValues(c: SwiftConsumer()) == "String,Int32,[String]")
}

// Swift-side subclasses and implementations used by the tests above.

private final class SwiftStringProducer: OpenAnyProducer {
    override func produceAny() -> any _KotlinBridgeable { "hello" }
}

private final class SwiftAnyProducerImpl: KotlinBase, AnyProducer {
    func produceAny() -> any _KotlinBridgeable { "from-swift" }
}

private final class SwiftGenericProducerImpl: KotlinBase, Producer {
    func produce() -> (any _KotlinBridgeable)? { Int32(5) }
}

private final class NullableInt32Source: NullableAnySource {
    let value: Int32?
    init(_ value: Int32?) { self.value = value; super.init() }
    override func get() -> (any _KotlinBridgeable)? { value }
}

// `initial` differs from the override's value, so the result proves Kotlin dispatched to Swift.
private final class GenericInt32Source: GenericSource {
    init() { super.init(initial: Int32(0)) }
    override func get() -> (any _KotlinBridgeable)? { Int32(21) }
}

private final class Int32Sink: AnySink {
    override func accept(x: any _KotlinBridgeable) -> String {
        if let i = x as? Int32 { return "Int32:\(i)" }
        return "other:\(x)"
    }
}

private final class SwiftConsumer: KotlinBase, Consumer {
    func consume(x: (any _KotlinBridgeable)?) -> String {
        switch x {
        case is String: "String"
        case is Int32: "Int32"
        case is [String]: "[String]"
        default: "other:\(String(describing: x.map { type(of: $0) }))"
        }
    }
}
