import Main
import Testing
import Foundation

// Cancellation is observed directly: the override reports a `CancellationError` it caught through `onCancelled`. The
// sleep is only an upper bound — a cancelled override returns as soon as it is cancelled, so a passing run never
// waits it out.
private final class SwiftAsync: AsyncGuarded, @unchecked Sendable {
    let nanos: UInt64
    let onCancelled: @Sendable () -> Void

    init(nanos: UInt64 = 10_000_000_000, onCancelled: @escaping @Sendable () -> Void = {}) {
        self.nanos = nanos
        self.onCancelled = onCancelled
        super.init()
    }

    override func guarded() async throws -> String {
        markStarted()
        do {
            try await Task.sleep(nanoseconds: nanos)
        } catch let error as CancellationError {
            onCancelled()
            throw error
        }
        return "swift"
    }
}

private func outcome(_ body: () async throws -> String) async -> Result<String, any Error> {
    do {
        return .success(try await body())
    } catch {
        return .failure(error)
    }
}

// `withContext(NonCancellable)` must keep the cancellation signal away from the override entirely
@Test
func nonCancellableRegionShieldsSwiftOverrideFromCancellation() async throws {
    #expect(try await callGuardedNonCancellable(g: SwiftAsync(nanos: 1_000_000_000)) == "swift")
}

// The scope is failed by a child throwing, and the override is awaited directly in the scope body.
@Test(.disabled("KT-88550: A Swift suspend override awaited in a coroutineScope body is not cancelled when a child coroutine fails"))
func failingChildDoesNotCancelSwiftOverrideAwaitedInScopeBody() async throws {
    let result = await confirmation("the Swift override was cancelled", expectedCount: 1) { confirm in
        await outcome { try await callFailingChildWithOverrideInScopeBody(g: SwiftAsync(onCancelled: { confirm() })) }
    }
    #expect(throws: ChildFailure.self) { try result.get() }
}

// The scope's own job is cancelled, and the override is awaited directly in the scope body.
@Test
func cancelledScopeJobCancelsSwiftOverrideInScopeBody() async throws {
    let result = await confirmation("the override under a cancelled scope job was cancelled", expectedCount: 1) { confirm in
        await outcome { try await callCancelledScopeJobWithOverrideInScopeBody(g: SwiftAsync(onCancelled: { confirm() })) }
    }
    #expect(throws: CancellationError.self) { try result.get() }
}

// Cancellation originating in a failing sibling and reaching an override awaited in a child coroutine
@Test
func failingChildCancelsSwiftOverrideInChildCoroutine() async throws {
    let result = await confirmation("the override in a child coroutine was cancelled", expectedCount: 1) { confirm in
        await outcome { try await callFailingChildWithOverrideInChild(g: SwiftAsync(onCancelled: { confirm() })) }
    }
    #expect(throws: ChildFailure.self) { try result.get() }
}
