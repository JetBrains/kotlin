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

