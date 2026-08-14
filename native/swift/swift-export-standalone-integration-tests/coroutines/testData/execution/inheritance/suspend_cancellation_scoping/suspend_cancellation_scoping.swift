import Main
import Testing
import Foundation

// Cancellation is observed as elapsed time: an override that was never cancelled burns its whole sleep, so the
// threshold only has to separate "returned promptly" from "slept for 3s".
private let cancelledPromptly: TimeInterval = 1.5

private final class SwiftAsync: AsyncGuarded {
    let nanos: UInt64

    init(nanos: UInt64 = 3_000_000_000) {
        self.nanos = nanos
        super.init()
    }

    override func guarded() async throws -> String {
        try await Task.sleep(nanoseconds: nanos)
        return "swift"
    }
}

private func elapsed(_ body: () async throws -> Void) async -> TimeInterval {
    let start = Date()
    _ = try? await body()
    return Date().timeIntervalSince(start)
}

// `withContext(NonCancellable)` must keep the cancellation signal away from the override entirely
@Test
func nonCancellableRegionShieldsSwiftOverrideFromCancellation() async throws {
    #expect(try await callGuardedNonCancellable(g: SwiftAsync(nanos: 300_000_000)) == "swift")
}

// The scope is failed by a child throwing, and the override is awaited directly in the scope body.
@Test(.disabled("KT-88550: A Swift suspend override awaited in a coroutineScope body is not cancelled when a child coroutine fails"))
func failingChildDoesNotCancelSwiftOverrideAwaitedInScopeBody() async throws {
    let seconds = await elapsed { _ = try await callFailingChildWithOverrideInScopeBody(g: SwiftAsync()) }
    #expect(seconds < cancelledPromptly, "the Swift override was not cancelled (elapsed \(seconds)s)")
}

// Cancellation originating in a failing sibling and the override is awaited directly in the scope body.
@Test
func cancelledScopeJobCancelsSwiftOverrideInScopeBody() async throws {
    let seconds = await elapsed { _ = try await callCancelledScopeJobWithOverrideInScopeBody(g: SwiftAsync()) }
    #expect(seconds < cancelledPromptly,
            "the override under a cancelled scope job was not cancelled (elapsed \(seconds)s)")
}

// Cancellation originating in a failing sibling and reaching an override awaited in a child coroutine
@Test
func failingChildCancelsSwiftOverrideInChildCoroutine() async throws {
    let seconds = await elapsed { _ = try await callFailingChildWithOverrideInChild(g: SwiftAsync()) }
    #expect(seconds < cancelledPromptly,
            "the override in a child coroutine was not cancelled (elapsed \(seconds)s)")
}
