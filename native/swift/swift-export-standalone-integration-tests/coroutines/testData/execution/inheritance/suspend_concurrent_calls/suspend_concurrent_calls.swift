import Main
import Testing
import Foundation

// Two Kotlin children, each dispatching into its own Swift override, both suspended at the same time
@Test
func concurrentKotlinChildrenReachSwiftOverridesSimultaneously() async throws {
    final class SwiftWorker: AsyncWorker, @unchecked Sendable {
        override func work() async throws -> String {
            try await Task.sleep(nanoseconds: 100_000_000)
            return "swift-work:\(id)"
        }
    }

    let result = try await callBothConcurrently(a: SwiftWorker(id: "a"), b: SwiftWorker(id: "b"))
    #expect(result == "swift-work:a|swift-work:b")

    // Plain Kotlin receivers on the same concurrent driver.
    #expect(try await callBothConcurrently(a: AsyncWorker(id: "a"), b: AsyncWorker(id: "b"))
            == "kotlin-work:a|kotlin-work:b")
}

// Reentrancy on a single receiver: two reverse-bridge calls into the *same* Swift instance concurrently
@Test
func swiftAsyncLetDrivesConcurrentCallsOnOneSwiftInstance() async throws {
    final class SwiftWorker: AsyncWorker, @unchecked Sendable {
        override func work() async throws -> String {
            try await Task.sleep(nanoseconds: 50_000_000)
            return "swift-work:\(id)"
        }
    }

    let w = SwiftWorker(id: "solo")
    async let first = callWork(w: w)
    async let second = callWork(w: w)
    let results = try await [first, second]
    #expect(results == ["swift-work:solo", "swift-work:solo"])
}
