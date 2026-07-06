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
    // Inherited Kotlin `count` reached via the direct-dispatch bridge (no recursion).
    #expect(try await callCount(base: derived) == 42)
}

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

// A Swift override that throws: the error must travel back through the reverse bridge's exception
// channel into the Kotlin coroutine and then out to the caller.
@Test
func swiftSuspendOverrideCanThrow() async throws {
    class SwiftThrower: AsyncThrower {
        struct Failure: Error {}
        override func boom() async throws -> String {
            throw Failure()
        }
    }

    let thrower = SwiftThrower()
    await #expect(throws: (any Error).self) {
        _ = try await callBoom(t: thrower)
    }
}

// A Swift override that throws an error carrying an explicit message: the message must survive the
// Swift -> Kotlin -> Swift round trip. The Swift `Error` type itself does not survive (it is bridged
// through `NSError`/`SwiftException`), so we assert on the message we put in `NSLocalizedDescriptionKey`.
@Test
func swiftSuspendOverrideThrowMessageSurvivesRoundTrip() async throws {
    class SwiftThrower: AsyncThrower {
        override func boom() async throws -> String {
            throw NSError(domain: "swift.test", code: 7, userInfo: [NSLocalizedDescriptionKey: "swift-boom-42"])
        }
    }

    let result = await Task<String, any Error>.detached {
        try await callBoom(t: SwiftThrower())
    }.result

    if case let .failure(e) = result {
        #expect(!(e is CancellationError))
        #expect(String(describing: e).contains("swift-boom-42"), "the Swift override's error message must survive the round trip")
    } else {
        Issue.record("expected the Swift override's error to propagate to the Kotlin caller")
    }
}

// Same as above, but for a Swift override of a Kotlin `suspend` *interface* method: the interface
// reverse bridge's exception channel must carry the error message too.
@Test
func swiftSuspendInterfaceOverrideThrowMessageSurvives() async throws {
    class ThrowingSpeaker: AsyncSpeakerBase {
        override func speak() async throws -> String {
            throw NSError(domain: "swift.test", code: 9, userInfo: [NSLocalizedDescriptionKey: "interface-boom"])
        }
    }

    let result = await Task<String, any Error>.detached {
        try await callSpeak(s: ThrowingSpeaker())
    }.result

    if case let .failure(e) = result {
        #expect(!(e is CancellationError))
        #expect(String(describing: e).contains("interface-boom"), "the interface override's error message must survive the round trip")
    } else {
        Issue.record("expected the Swift interface override's error to propagate")
    }
}

// A Swift override that calls `super`, where the Kotlin super implementation throws. The Kotlin-origin
// exception flows out through the Swift override frame (Kotlin -> Swift via the `_direct` forward bridge,
// then Swift -> Kotlin via the reverse bridge's exception channel) back to the original caller, where it
// surfaces as a thrown, non-cancellation error.
//
// KNOWN GAP: the original Kotlin message ("kotlin-boom") does NOT survive this Kotlin -> Swift -> Kotlin
// double-cross. A single forward crossing preserves the message (cf. `testThrowing`), but here the Kotlin
// exception is wrapped as a Swift `KotlinError` on the way out and then re-bridged to `NSError` on the way
// back; `KotlinError` does not expose the underlying message via `CustomNSError`/`LocalizedError`, so the
// caller only sees a generic `KotlinError` ("...KotlinRuntimeSupport.KotlinError error 1..."). We therefore
// only assert that a non-cancellation error propagates.
// TODO: tighten to assert the message contains "kotlin-boom" once Kotlin exception identity/message is
//  preserved across the round trip (would require `KotlinError` to conform to `CustomNSError`).
@Test
func swiftSuspendOverrideRethrowsKotlinSuperException() async throws {
    class PassThrough: AsyncThrower {
        override func boom() async throws -> String {
            return try await super.boom() // Kotlin super throws AsyncException("kotlin-boom")
        }
    }

    let result = await Task<String, any Error>.detached {
        try await callBoom(t: PassThrough())
    }.result

    if case let .failure(e) = result {
        #expect(!(e is CancellationError), "a non-cancellation error must propagate out through the Swift override frame")
    } else {
        Issue.record("expected the Kotlin super exception to propagate")
    }
}

// Inward cancellation: a Swift Task drives a Kotlin suspend call that virtually dispatches into a Swift
// override which suspends. Cancelling the Task must propagate through the forward bridge, the Kotlin
// coroutine, and the reverse bridge into the override's `Task.sleep`, surfacing as a cancellation error.
@Test
func swiftSuspendOverrideObservesInwardCancellation() async throws {
    final class SleepingDerived: AsyncBase, @unchecked Sendable {
        override func greet(name: String) async throws -> String {
            try await Task.sleep(nanoseconds: 10_000_000_000)
            return "Swift: \(name)"
        }
    }

    let derived = SleepingDerived()
    let task = Task<String, any Error>.detached {
        try await callGreet(base: derived, name: "X")
    }
    DispatchQueue.global().asyncAfter(deadline: .now() + 0.1) {
        task.cancel()
    }

    let result = await task.result
    #expect(task.isCancelled)
    #expect(result == .failure(CancellationError()), "inward cancellation must reach the Swift override and surface as a cancellation error")
}

// Kotlin-side cancellation crossing the reverse bridge: a Kotlin `withTimeoutOrNull` wraps a call that
// dispatches into a Swift override sleeping far longer than the timeout. The Kotlin timeout must cancel
// the Swift override, so the call returns the timeout sentinel quickly instead of after the full sleep.
@Test
func kotlinTimeoutCancelsSwiftSuspendOverride() async throws {
    class SlowDerived: AsyncBase {
        override func greet(name: String) async throws -> String {
            try await Task.sleep(nanoseconds: 10_000_000_000)
            return "Swift: \(name)"
        }
    }

    let start = Date()
    let result = try await callGreetWithTimeout(base: SlowDerived(), name: "X", timeoutMs: 100)
    #expect(result == "timed_out")
    #expect(Date().timeIntervalSince(start) < 5.0, "the Kotlin timeout must cancel the Swift override promptly")
}

// Cancellation cleanup: the Swift override's `defer` (its unwind/cleanup path) must run when the call is
// cancelled mid-suspension through the reverse bridge.
@Test
func swiftSuspendOverrideRunsCleanupOnCancellation() async throws {
    final class CleanupDerived: AsyncBase, @unchecked Sendable {
        let onCleanup: @Sendable () -> Void
        init(onCleanup: @escaping @Sendable () -> Void) {
            self.onCleanup = onCleanup
            super.init()
        }
        override func greet(name: String) async throws -> String {
            defer { onCleanup() }
            try await Task.sleep(nanoseconds: 10_000_000_000)
            return "Swift: \(name)"
        }
    }

    let result: Result<String, any Error> = await confirmation("override cleanup ran on cancellation", expectedCount: 1) { confirm in
        let derived = CleanupDerived(onCleanup: { confirm() })
        let task = Task<String, any Error>.detached {
            try await callGreet(base: derived, name: "X")
        }
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.1) {
            task.cancel()
        }
        return await task.result
    }

    #expect(result == .failure(CancellationError()))
}

func ==<T>(_ lhs: Result<T, any Error>, _ rhs: Result<T, any Error>) -> Bool where T: Equatable {
    switch (lhs, rhs) {
    case (.success(let l), .success(let r)): l == r
    case (.failure(let l), .failure(let r)): (l as any Equatable).equals(r)
    default: false
    }
}

extension Equatable {
    func equals(_ other: Any) -> Bool {
        (other as? Self).map { self == $0 } ?? false
    }
}
