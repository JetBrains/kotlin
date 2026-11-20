import UnboundGenerics
import KotlinRuntime
import KotlinRuntimeSupport
import Testing

// KT-80260: values travelling through `Any` / an unbound type parameter (`any _KotlinBridgeable` in Swift).
// Kotlin reports what it actually received with `describeAny`.

@Test
func kotlinReadsSwiftOverriddenProperty() throws {
    let h = SwiftItemHolder()
    #expect(readItem(h: h) == "Int:7")
    #expect(readItems(h: h) == "List[Int:7]")  // Kotlin's `items` wraps the Swift override, not the base value
    #expect(callGet(h: h) == "Int:7")
}

@Test
func kotlinCallsSwiftOverriddenMethod() throws {
    #expect(callGet(h: SwiftGetHolder()) == "String:swift")
}

@Test
func swiftOverrideReceivesKotlinValueAsNativeString() throws {
    let h = SwiftTransformHolder()
    #expect(callTransformWithKotlinValue(h: h) == "String:from-swift")
    #expect(h.received == "from-kotlin")
}

@Test
func swiftSubclassKeepsIdentityThroughT() throws {
    let h = SwiftGetHolder()
    #expect((echoT(x: h) as? SwiftGetHolder) === h, "T lost the Swift subclass")
    #expect((echoAny(x: h) as? SwiftGetHolder) === h, "Any lost the Swift subclass")
}

@Test
func genericSwiftSubclassKeepsSpecialization() throws {
    let h = STypedHolder(fixed: 2)
    #expect(callGet(h: h) == "String:2")
    #expect((echoT(x: h) as? STypedHolder<Int>) === h, "Int specialization lost")
    #expect(echoT(x: h) as? STypedHolder<String> == nil, "cast to the wrong specialization succeeded")
}

// Swift-side subclasses and implementations used by the tests above.

private final class SwiftGetHolder: Holder {
    init() { super.init(item: "base") }
    override func get() -> (any _KotlinBridgeable)? { "swift" }
}

private final class SwiftItemHolder: Holder {
    init() { super.init(item: "base") }
    override var item: (any _KotlinBridgeable)? { Int32(7) }
}

private final class SwiftTransformHolder: Holder {
    var received: String?
    init() { super.init(item: "base") }
    override func transform(x: (any _KotlinBridgeable)?) -> (any _KotlinBridgeable)? {
        received = x as? String
        return "from-swift"
    }
}

private final class STypedHolder<T: CustomStringConvertible>: Holder {
    let fixed: T
    init(fixed: T) { self.fixed = fixed; super.init(item: "base") }
    override func get() -> (any _KotlinBridgeable)? { fixed.description }
}
