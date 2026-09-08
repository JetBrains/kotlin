import Main
import Testing
import Foundation

// One Swift class overriding members of two different Kotlin suspend interfaces
@Test
func swiftClassPatchesTwoSuspendInterfaceSlots() async throws {
    class SwiftMulti: AsyncMultiBase {
        override func left() async throws -> String { "Swift left" }
        override func right() async throws -> String { "Swift right" }
    }

    let value = SwiftMulti()
    #expect(try await callAsyncLeft(value: value) == "Swift left")
    #expect(try await callAsyncRight(value: value) == "Swift right")

    // The plain Kotlin class keeps both Kotlin implementations.
    let base = AsyncMultiBase()
    #expect(try await callAsyncLeft(value: base) == "Kotlin left")
    #expect(try await callAsyncRight(value: base) == "Kotlin right")
}

@Test
func constructedSuspendSubclassIsRetainedAndCancelled() async throws {
    class StoredWorker: ConstructedAsyncBase {
        override func work() async throws -> String {
            try await Task.sleep(nanoseconds: 10_000_000_000)
            return "Swift work"
        }
    }

    let value = StoredWorker(code: 7)
    let storage = ConstructedAsyncStorage()
    storage.store(value: value)
    let retrieved = try #require(storage.retrieve())
    #expect(retrieved === value)
    #expect(retrieved.origin == "code:7")
    #expect(try await callConstructedWorkWithTimeout(value: retrieved, timeoutMs: 100) == "timed_out")
}
