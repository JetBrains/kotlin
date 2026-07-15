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

// A Swift override of a Kotlin `suspend` method with a `vararg` parameter. The reverse async bridge
// must call the variadic Swift method with the runtime array it received from Kotlin.
@Test
func swiftCanOverrideKotlinSuspendVarargMethod() async throws {
    class SwiftVararg: AsyncVararg {
        override func join(parts: String...) async throws -> String {
            return "Swift: " + parts.joined(separator: ",")
        }
    }

    #expect(try await callJoin(v: SwiftVararg()) == "Swift: a,b,c")
    // The original Kotlin class is untouched.
    #expect(try await callJoin(v: AsyncVararg()) == "Kotlin: a,b,c")
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

// A Swift override that throws: with RCRef error carriage the Swift error round-trips
// Swift -> Kotlin -> Swift back to ITSELF (the reverse bridge boxes it as a Kotlin `SwiftError`; the
// forward bridge unwraps that box), so both its identity (`NSError`) and message survive.
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
        #expect(e is NSError, "the Swift override's error identity must survive the round trip")
        #expect(String(describing: e).contains("swift-boom-42"), "the Swift override's error message must survive the round trip")
    } else {
        Issue.record("expected the Swift override's error to propagate to the Kotlin caller")
    }
}

// Same as above, but for a Swift override of a Kotlin `suspend` *interface* method: the interface
// reverse bridge's exception channel must carry the error identity and message too.
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
        #expect(e is NSError, "the interface override's error identity must survive the round trip")
        #expect(String(describing: e).contains("interface-boom"), "the interface override's error message must survive the round trip")
    } else {
        Issue.record("expected the Swift interface override's error to propagate")
    }
}

// A Swift override that calls `super`, where the Kotlin super implementation throws. With RCRef error
// carriage the ORIGINAL Kotlin exception round-trips Kotlin -> Swift(super) -> Kotlin -> Swift with its
// identity AND message intact: forward it is thrown transparently as the exported `AsyncException`;
// reverse `kotlinThrowableRCRef` recognizes the Kotlin throwable coming home and re-raises the original.
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
        #expect(e is AsyncException, "the original Kotlin exception type must survive the round trip")
        #expect(String(describing: e).contains("kotlin-boom"), "the original Kotlin exception message must survive the round trip")
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

// Swift's cancellation is advisory: a Swift override may observe cancellation, decline to rethrow it, and still
// return a value. That value must propagate to the Kotlin caller (Swift semantics) instead of being replaced by a
// CancellationError. Under the old eager reverse bridge the Kotlin caller resumed on cancellation and this value was
// discarded; the honest bridge awaits the override and delivers what it actually produced.
@Test
func swiftSuspendOverrideValueSurvivesCancellation() async throws {
    final class UncooperativeDerived: AsyncBase, @unchecked Sendable {
        override func greet(name: String) async throws -> String {
            try? await Task.sleep(nanoseconds: 10_000_000_000) // cancellation aborts the sleep but is swallowed
            return "Swift: \(name)"
        }
    }

    let derived = UncooperativeDerived()
    let task = Task<String, any Error>.detached {
        try await callGreet(base: derived, name: "X")
    }
    DispatchQueue.global().asyncAfter(deadline: .now() + 0.1) {
        task.cancel()
    }

    let result = await task.result
    #expect(task.isCancelled)
    #expect(result == .success("Swift: X"), "a value returned by the override after cancellation must reach the Kotlin caller")
}

// Same as above, but the non-cooperative override throws its OWN error after cancellation instead of returning; the
// custom error (its message) must survive to the Kotlin caller rather than being replaced by a CancellationError.
@Test
func swiftSuspendOverrideCustomErrorSurvivesCancellation() async throws {
    final class UncooperativeThrower: AsyncBase, @unchecked Sendable {
        override func greet(name: String) async throws -> String {
            try? await Task.sleep(nanoseconds: 10_000_000_000)
            throw NSError(domain: "swift.test", code: 5, userInfo: [NSLocalizedDescriptionKey: "uncooperative-boom"])
        }
    }

    let derived = UncooperativeThrower()
    let task = Task<String, any Error>.detached {
        try await callGreet(base: derived, name: "X")
    }
    DispatchQueue.global().asyncAfter(deadline: .now() + 0.1) {
        task.cancel()
    }

    let result = await task.result
    #expect(task.isCancelled)
    if case let .failure(e) = result {
        #expect(!(e is CancellationError), "the override's own error must survive, not be replaced by cancellation")
        #expect(String(describing: e).contains("uncooperative-boom"))
    } else {
        Issue.record("expected the override's custom error to propagate, got \(result)")
    }
}

// Forward transparency: a pure Kotlin `suspend` fn that throws must surface to a Swift `async` caller as
// its concrete exported exception type (pattern-matchable), not an opaque wrapper.
@Test
func swiftAsyncCatchesConcreteKotlinException() async throws {
    do {
        _ = try await callBoom(t: AsyncThrower())
        Issue.record("expected AsyncException to be thrown")
    } catch let e as AsyncException {
        #expect(String(describing: e).contains("kotlin-boom"))
    }
}

// A Swift value-type error thrown by an override must round-trip Swift -> Kotlin -> Swift back to itself
// (boxed as a Kotlin `SwiftError` on the way in, unwrapped on the way out).
@Test
func swiftAsyncErrorRoundTripsBackToSwift() async throws {
    struct MySwiftError: Error, Equatable { let code: Int }
    final class Thrower: AsyncThrower, @unchecked Sendable {
        override func boom() async throws -> String { throw MySwiftError(code: 42) }
    }

    let result = await Task<String, any Error>.detached {
        try await callBoom(t: Thrower())
    }.result

    if case let .failure(e) = result {
        #expect(e is MySwiftError, "a Swift value-type error must round-trip back to Swift as itself")
        #expect((e as? MySwiftError)?.code == 42)
    } else {
        Issue.record("expected the Swift value-type error to propagate")
    }
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
