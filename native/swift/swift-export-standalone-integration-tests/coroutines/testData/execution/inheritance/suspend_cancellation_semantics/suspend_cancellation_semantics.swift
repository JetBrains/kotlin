import Main
import Testing
import Foundation

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

// Equality for `Result` so cancellation outcomes can be compared directly.
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
