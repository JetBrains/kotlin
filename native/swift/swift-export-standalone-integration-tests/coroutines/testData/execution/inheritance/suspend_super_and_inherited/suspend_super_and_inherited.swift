import Main
import Testing
import Foundation

// The non-virtual ("_direct") forward async bridge also backs `super` calls from a Swift override.
@Test
func swiftSuspendOverrideCanCallSuper() async throws {
    class WrappingDerived: AsyncBase {
        override func greet(name: String) async throws -> String {
            let inner = try await super.greet(name: name)
            return "wrapped(\(inner))"
        }
    }

    let derived = WrappingDerived()
    #expect(try await derived.greet(name: "Y") == "wrapped(Kotlin: Y)")
    #expect(try await callGreet(base: derived, name: "Z") == "wrapped(Kotlin: Z)")
}

// The non-virtual ("_direct") forward async bridge: a Swift subclass that overrides only `greet`
// must still be able to inherit `count` without infinitely recursing through the patched vtable slot.
@Test
func swiftSubclassInheritsNonOverriddenSuspendMethod() async throws {
    class PartialDerived: AsyncBase {
        override func greet(name: String) async throws -> String { "Swift: \(name)" }
        // count() intentionally NOT overridden.
    }

    let derived = PartialDerived()
    #expect(try await callGreet(base: derived, name: "X") == "Swift: X")
    // Inherited Kotlin `count` reached via the direct-dispatch bridge (no recursion): the slot exists but is
    // left unpatched.
    #expect(try await callCount(base: derived) == 42)
    // `notOpen` is final, so there is no slot to patch in the first place; the Swift subclass must still
    // reach the Kotlin body.
    #expect(try await callNotOpen(base: derived) == "kotlin-final")
}

// A Swift class that inherits a Kotlin class and first-adopts a Kotlin `suspend`-interface, inheriting
// its DEFAULT method (does not override `describe`). The inherited default must dispatch non-virtually
// (via the `_direct` async bridge) so it never recurses through the patched itable; its open self-call
// to `tag()` must reach the Swift override.
@Test
func swiftInheritsKotlinSuspendInterfaceDefault() async throws {
    class MyAsyncDefaulter: AsyncSpeakerBase, AsyncDefaulter {
        func tag() async throws -> String { "swift-tag" }
        // describe() intentionally NOT overridden -> inherits the Kotlin async default.
    }
    let d = MyAsyncDefaulter()

    // Direct Swift dispatch: inherited async default runs; its open self-call reaches the Swift override.
    #expect(try await d.describe() == "default-describe(swift-tag)")
    // Kotlin-side dispatch must terminate (no infinite recursion) and yield the same result.
    #expect(try await callAsyncDescribe(d: d) == "default-describe(swift-tag)")
}
