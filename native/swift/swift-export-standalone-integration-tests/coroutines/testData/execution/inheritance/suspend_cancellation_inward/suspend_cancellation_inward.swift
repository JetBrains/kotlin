import Main
import Testing
import Foundation

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
