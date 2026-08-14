import Main
import Testing
import Foundation

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
