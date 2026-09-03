import Inheritance
import Testing

// The reproducer from KT-88259: a bare Swift subclass of an exported Kotlin class that overrides
// nothing at all. Before the fix, every one of these calls exhausted the stack.
@Test
func swiftSubclassInheritsAnyMembersFromKotlin() throws {
    class AnySw: Some {}

    let value = AnySw()
    let other = AnySw()

    // kotlin.Any.toString() renders the runtime class name, which for a Swift subclass is its
    // mangled Obj-C name -- so pin the invariant rather than the spelling: Kotlin's `toString()`
    // and Obj-C's `-description` are the same slot and must produce the same string.
    let rendered = callToString(value: value)
    #expect(!rendered.isEmpty)
    #expect(rendered == String(describing: value))

    // kotlin.Any.hashCode() is the identity hash, and `-hash` bridges the same slot.
    #expect(Swift.Int(callHashCode(value: value)) == value.hash)
    #expect(callHashCode(value: value) == callHashCode(value: value))

    // kotlin.Any.equals() is reference equality. Distinct receivers are the interesting case:
    // -[KotlinBase isEqual:] short-circuits on identity before reaching Kotlin.
    #expect(callEquals(lhs: value, rhs: value))
    #expect(!callEquals(lhs: value, rhs: other))
    #expect(!value.isEqual(other))
}

// Overriding an open Kotlin member must not disturb the inherited kotlin.Any members: the reverse
// bridge for `greet` and kotlin.Any's adapters occupy different slots of the same vtable.
@Test
func swiftSubclassOverridingKotlinMemberKeepsAnyMembers() throws {
    class SwiftGreeter: Greeter {
        override func greet() -> Swift.String { "swift-greeting" }
    }

    let value = SwiftGreeter()
    #expect(callGreet(greeter: value) == "swift-greeting")
    #expect(callToString(value: value) == String(describing: value))
    #expect(Swift.Int(callHashCode(value: value)) == value.hash)
    #expect(!callEquals(lhs: value, rhs: SwiftGreeter()))
}

// A two-level Swift hierarchy: Kotlin_SwiftExport_getOrCreateTypeInfoForSwiftSubclass collapses the
// whole Swift chain onto the Kotlin supertype, so the walk to the closest compiled-Kotlin TypeInfo
// has to cope with more than one synthesized level.
@Test
func twoLevelSwiftHierarchyInheritsAnyMembers() throws {
    class FirstLevel: Some {}
    class SecondLevel: FirstLevel {}

    let value = SecondLevel()
    #expect(callToString(value: value) == String(describing: value))
    #expect(Swift.Int(callHashCode(value: value)) == value.hash)
    #expect(!callEquals(lhs: value, rhs: FirstLevel()))
}

// Obj-C-level overrides are honoured, and `super` reaches Kotlin instead of looping: the KotlinBase
// implementation bypasses the adapter that would send it back to `-description`.
@Test
func swiftSubclassCanOverrideObjCDescriptionAndCallSuper() throws {
    class Described: Some {
        override var description: Swift.String { "swift:" + super.description }
    }

    let value = Described()
    let kotlinRendering = String(describing: Some())

    #expect(value.description.hasPrefix("swift:"))
    #expect(!kotlinRendering.isEmpty)

    // Kotlin's toString() routes through -description, so it sees the Swift override too.
    #expect(callToString(value: value) == value.description)
}
