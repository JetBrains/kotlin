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

    // kotlin.Any.equals() is reference equality. Distinct receivers are the interesting case:
    // -[KotlinBase isEqual:] short-circuits on identity before reaching Kotlin.
    #expect(callEquals(lhs: value, rhs: value))
    #expect(!callEquals(lhs: value, rhs: other))
    #expect(!value.isEqual(other))
    #expect(!other.isEqual(value))
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

// `description`/`hash`/`isEqual:` are the native extension points for kotlin.Any's members: an
// exported class is rooted in NSObject, and kotlin.Any's reverse adapters send exactly those
// selectors. Overriding them in Swift must therefore be what Kotlin dispatch observes.
@Test
func swiftSubclassOverridingNativeAnyMembers() throws {
    class Described: Some {
        override var description: Swift.String { "swift-described" }
        override var hash: Swift.Int { 42 }
        override func isEqual(_ other: Any?) -> Swift.Bool { other is Described }
    }

    let value = Described()
    let other = Described()

    #expect(callToString(value: value) == "swift-described")
    #expect(callHashCode(value: value) == 42)
    #expect(callEquals(lhs: value, rhs: other))
    #expect(!callEquals(lhs: value, rhs: Some()))
}

// The same overrides, but delegating to `super`: the KotlinBase implementation has to reach the
// compiled kotlin.Any member instead of re-sending the selector it was entered through.
@Test
func swiftSubclassCallingSuperOfNativeAnyMembers() throws {
    class Described: Some {
        override var description: Swift.String { "swift+" + super.description }
        override var hash: Swift.Int { super.hash }
        override func isEqual(_ other: Any?) -> Swift.Bool { super.isEqual(other) }
    }

    let value = Described()
    let other = Described()

    // kotlin.Any.toString() renders "$className@$hashCodeStr"; the prefix contributes no "@".
    let rendered = String(describing: value)
    #expect(rendered.hasPrefix("swift+"))
    #expect(rendered.contains("@"))

    // Kotlin's toString() routes through -description, so it sees the Swift override too.
    #expect(callToString(value: value) == rendered)

    // kotlin.Any.toString() calls hashCode() virtually, so the rendering above also went through
    // `super.hash`.
    #expect(Swift.Int(callHashCode(value: value)) == value.hash)

    // `super.isEqual:` reaches kotlin.Any's reference equality.
    #expect(value.isEqual(value))
    #expect(!value.isEqual(other))
    #expect(callEquals(lhs: value, rhs: value))
    #expect(!callEquals(lhs: value, rhs: other))
}

// kotlin.Any.toString() renders "$className@$hashCodeStr" and calls hashCode() virtually, so a
// Swift subclass that overrides only `hash` has to be visible inside the rendering itself.
@Test
func kotlinAnyToStringSeesTheSwiftHashOverride() throws {
    class Hashed: Some {
        override var hash: Swift.Int { 42 }
    }

    let value = Hashed()

    #expect(callHashCode(value: value) == 42)

    let rendered = callToString(value: value)
    #expect(rendered == String(describing: value))
    #expect(rendered.hasSuffix("@2a")) // kotlin.Any renders the hash as unsigned hex.
}

// kotlin.Any.hashCode() is a 32-bit Int, while -hash is NSUInteger-wide. The bridge does not drop
// the high word: it folds the halves together, as `MethodBridge.ReturnValue.HashCode` does in
// ObjCExportCodeGenerator (`low xor high` whenever NSInteger is 64-bit).
@Test
func wideSwiftHashIsFoldedIntoKotlinInt() throws {
    class WideHash: Some {
        override var hash: Swift.Int { (1 << 32) | 7 }
    }

    let value = WideHash()

    #expect(value.hash == (1 << 32) | 7)
    #expect(callHashCode(value: value) == 7 ^ 1)
}
