import Main
import Testing
import Foundation

// A Swift subclass overriding a Kotlin `suspend` method. Dispatching to the override happens through
// the reverse async bridge: Kotlin's virtual call hits the patched vtable trampoline, which suspends
// the Kotlin coroutine and awaits the Swift override via a Task.
@Test
func swiftCanOverrideKotlinSuspendMethod() async throws {
    class SwiftDerived: AsyncBase {
        override func greet(name: String) async throws -> String {
            return "Swift: \(name)"
        }
    }

    let derived = SwiftDerived()

    // Direct Swift dispatch.
    #expect(try await derived.greet(name: "A") == "Swift: A")
    // Call through Kotlin: the reverse async bridge must dispatch to the Swift override.
    #expect(try await callGreet(base: derived, name: "B") == "Swift: B")

    // The original Kotlin class is untouched.
    let base = AsyncBase()
    #expect(try await callGreet(base: base, name: "C") == "Kotlin: C")
}

// Swift overriding a Kotlin `suspend` *interface* method, reached via Kotlin-side interface dispatch.
@Test
func swiftCanOverrideKotlinSuspendInterfaceMethod() async throws {
    class SwiftSpeaker: AsyncSpeakerBase {
        override func speak() async throws -> String { "Swift speaks" }
    }

    let speaker = SwiftSpeaker()
    #expect(try await callSpeak(s: speaker) == "Swift speaks")

    let base = AsyncSpeakerBase()
    #expect(try await callSpeak(s: base) == "Kotlin speaks")
}

// One Swift subclass overriding a suspend member and a plain member of the same Kotlin class. The suspend
// override is bound to an async reverse bridge, the plain one to an ordinary synchronous bridge
@Test
func swiftSubclassOverridesBothSuspendAndNonSuspendMembers() async throws {
    class MixedDerived: AsyncBase {
        override func greet(name: String) async throws -> String { "Swift: \(name)" }
        override func syncTag() -> String { "swift-sync" }
    }

    let d = MixedDerived()
    #expect(try await callGreet(base: d, name: "X") == "Swift: X")
    #expect(callSyncTag(base: d) == "swift-sync")

    // Overriding only the suspend member must leave the synchronous slot on the Kotlin body.
    class SuspendOnlyDerived: AsyncBase {
        override func greet(name: String) async throws -> String { "Swift: \(name)" }
    }
    #expect(callSyncTag(base: SuspendOnlyDerived()) == "kotlin-sync")
}

// A Unit-returning override resumes the Kotlin coroutine with no value to carry, so the side effect is the
// only evidence the override ran at all.
@Test
func swiftOverrideReturnsUnitThroughContinuation() async throws {
    final class Recorder: AsyncPayloads, @unchecked Sendable {
        var ran = false
        override func nothingToReturn() async throws { ran = true }
    }

    let r = Recorder()
    try await callNothingToReturn(p: r)
    #expect(r.ran, "a Unit-returning override must still resume the Kotlin coroutine")
}

// An unboxed primitive payload rather than an object reference.
@Test
func swiftOverrideReturnsPrimitiveThroughContinuation() async throws {
    final class Counter: AsyncPayloads, @unchecked Sendable {
        override func number() async throws -> Int32 { 42 }
    }

    #expect(try await callNumber(p: Counter()) == 42)
    #expect(try await callNumber(p: AsyncPayloads()) == 1)
}

// An exported Kotlin object created in Swift, and the nil case, both crossing back as the continuation value.
@Test
func swiftOverrideReturnsExportedObjectThroughContinuation() async throws {
    final class Maker: AsyncPayloads, @unchecked Sendable {
        override func payload() async throws -> AsyncPayload? { AsyncPayload(label: "swift") }
    }
    final class Nuller: AsyncPayloads, @unchecked Sendable {
        override func payload() async throws -> AsyncPayload? { nil }
    }

    #expect(try await callPayload(p: Maker())?.label == "swift")
    #expect(try await callPayload(p: Nuller()) == nil)
    #expect(try await callPayload(p: AsyncPayloads())?.label == "kotlin")
}
