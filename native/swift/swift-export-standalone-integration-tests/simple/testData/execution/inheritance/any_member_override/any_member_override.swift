import Inheritance
import Testing

@Test
func swiftOverridesKotlinOverridesOfAnyMembers() throws {
    class SwiftDescribable: Describable {
        override func toString() -> Swift.String { "swift-describable" }
        override func hashCode() -> Swift.Int32 { 22 }
    }

    let swiftValue = SwiftDescribable()

    // Kotlin dispatch must reach the Swift overrides, not the Kotlin implementations they shadow.
    #expect(callToString(value: swiftValue) == "swift-describable")
    #expect(callHashCode(value: swiftValue) == 22)

    // `description` and `hash` are the Obj-C spelling of the same slots, so they must agree.
    #expect(String(describing: swiftValue) == "swift-describable")
    #expect(swiftValue.hash == 22)

    // A plain Kotlin instance keeps the Kotlin implementations.
    let kotlinValue = Describable()
    #expect(callToString(value: kotlinValue) == "kotlin-describable")
    #expect(callHashCode(value: kotlinValue) == 11)
    #expect(String(describing: kotlinValue) == "kotlin-describable")
    #expect(kotlinValue.hash == 11)
}

@Test
func swiftSubclassInheritsKotlinOverridesOfAnyMembers() throws {
    // No overrides at all: the inherited open members must still reach Kotlin without recursing
    // through the reverse bridge installed in the subclass's own vtable slots.
    class InheritingDescribable: Describable {}

    let value = InheritingDescribable()
    #expect(callToString(value: value) == "kotlin-describable")
    #expect(callHashCode(value: value) == 11)
    #expect(String(describing: value) == "kotlin-describable")
    #expect(value.hash == 11)
}
