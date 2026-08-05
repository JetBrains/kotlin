import Main
import Testing
import Foundation

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
