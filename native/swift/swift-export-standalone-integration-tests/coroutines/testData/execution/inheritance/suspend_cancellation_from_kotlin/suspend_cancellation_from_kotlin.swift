import Main
import Testing
import Foundation

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

// The timeout does not wrap the Swift override directly: Kotlin's own default
// `describe()` body sits in between, and the suspending Swift `tag()` is reached from inside it.
// `describe()` is deliberately not overridden
@Test
func kotlinTimeoutCancelsSwiftOverrideCalledFromInheritedSuspendDefault() async throws {
    class TimedDefault: AsyncAnchor, AsyncDefaulter {
        func tag() async throws -> String {
            try await Task.sleep(nanoseconds: 10_000_000_000)
            return "swift-tag"
        }
        // describe() is inherited from AsyncDefaulter.
    }

    let start = Date()
    let result = try await callAsyncDescribeWithTimeout(d: TimedDefault(), timeoutMs: 100)
    #expect(result == "timed_out")
    #expect(Date().timeIntervalSince(start) < 5.0)
}
