import Main
import Testing
import Foundation

@Test
func testCallingKotlinThatUsesCoroutines() async throws {
    try #expect(await testPrimitive() == 42)
    try #expect(await testAny() as? Foo == Foo.shared)
    try #expect(await testObject() == Foo.shared)
    try #expect(await testCustom() == "Hello, World!")
}

@Test
func testNoCancellation() async {
    let task = Task<Int32, any Error>.detached {
        try await confirmation("called back after cancellation", expectedCount: 1) { confirm in
            return try await callAfter(delay: 300) {
                confirm()
                return 42
            }
        }
    }

    let result = await task.result

    #expect(!task.isCancelled)
    #expect(result == .success(42), "function should complete successfully with result of 42")
}

@Test
func testImmediateCancellation() async {
    let task = Task<Int32, any Error>.detached {
        return try await cancelImmediately()
    }

    let result = await task.result

    #expect(task.isCancelled)
    #expect(result == .failure(CancellationError()), "function should fail with cancellation error")
}

@Test
func testPrecedingCancellation() async {
    let task = Task<Int32, any Error>.detached {
        try await Task.sleep(nanoseconds: 1_000_000)
        return try await callAfter(delay: 0) {
            Issue.record("Callback shouldn't be invoked")
            return 1
        }
    }
    task.cancel()

    let result = await task.result

    #expect(task.isCancelled)
    #expect(result == .failure(CancellationError()), "function should fail with cancellation error")
}

@Test
func testInwardCancellation() async {
    let task = Task<Int32, any Error>.detached {
        return try await callAfter(delay: 3000) {
            Issue.record("Callback shouldn't be invoked")
            return 1
        }
    }

    DispatchQueue.global().asyncAfter(deadline: .now() + 1) {
        task.cancel()
    }

    let result = await task.result

    #expect(task.isCancelled)
    #expect(result == .failure(CancellationError()), "function should fail with cancellation error")
}

@Test
func testOutwardCancellation() async {
    let task = Task<Int32, any Error>.detached {
        return try await cancelAfter(delay: 3000)
    }

    let result = await task.result

    #expect(task.isCancelled)
    #expect(result == .failure(CancellationError()), "function should fail with cancellation error")
}

@Test
func testSilentOutwardCancellation() async {
    let task = Task<Int32, any Error>.detached {
        try await confirmation("called back after cancellation", expectedCount: 1) { confirm in
            return try await cancelSilentlyAfter(delay: 3000) {
                confirm()
                return 42
            }
        }
    }

    let result = await task.result

    #expect(task.isCancelled)
    #expect(result == .success(42), "function should complete successfully with result of 42")
}

@Test
func testThrowing() async {
    let task = Task<Int32, any Error>.detached {
        return try await throwAfter(delay: 1000, message: "Foo")
    }

    let result = await task.result

    if case let .failure(e) = result {
        #expect(String(describing: e).contains("Foo"), "function should fail with specific exception")
    } else {
        Issue.record("function should fail with non-cancellation error")
    }
}

@Test
func testImmediateThrowing() async {
    let task = Task<Int32, any Error>.detached {
        return try await throwImmediately(message: "Foo")
    }

    let result = await task.result

    if case let .failure(e) = result {
        #expect(String(describing: e).contains("Foo"), "function should fail with specific exception")
    } else {
        Issue.record("function should fail with non-cancellation error")
    }
}

@Test
func testNonExceptionThrowing() async {
    let task = Task<Int32, any Error>.detached {
        return try await throwNonException(message: "Foo")
    }

    let result = await task.result

    if case let .failure(e) = result {
        #expect(String(describing: e).contains("Foo"), "function should fail with specific exception")
    } else {
        Issue.record("function should fail with non-cancellation error")
    }
}

@Test
func testNeverCompletesIsCancellable() async throws {
    let task = Task<Int32, any Error>.detached {
        return try await neverCompletes()
    }

    try await Task.sleep(nanoseconds: 50_000_000)
    task.cancel()

    let result = await task.result
    #expect(task.isCancelled)
    #expect(result == .failure(CancellationError()))
}

@Test
func testMultipleCancelsAreIdempotent() async throws {
    let task = Task<Int32, any Error>.detached {
        return try await neverCompletes()
    }

    try await Task.sleep(nanoseconds: 50_000_000)
    task.cancel()
    task.cancel()
    task.cancel()
    task.cancel()

    let result = await task.result
    #expect(task.isCancelled)
    #expect(result == .failure(CancellationError()))
}

@Test
func testParentCancellationPropagatesToStructuredChild() async throws {
    let parent = Task<Int32, any Error>.detached {
    async let child: Int32 = try await neverCompletes()
        return try await child
    }

    try await Task.sleep(nanoseconds: 50_000_000)
    parent.cancel()

    let result = await parent.result
    #expect(parent.isCancelled)
    #expect(result == .failure(CancellationError()))
}

@Test
func testCancellationRaceLoop() async throws {
    let cancelDelaysMs: [UInt64] = [0, 1, 5, 10, 20, 35, 50, 75, 100, 150]

    let kotlinDelayMs: Int64 = 2_000

    try await withThrowingTaskGroup(of: Void.self) { group in
        for (i, cancelDelayMs) in cancelDelaysMs.enumerated() {
            group.addTask {
                let task = Task<Int32, any Error>.detached {
                    return try await callAfter(delay: kotlinDelayMs) {
                        Issue.record("Iteration \(i): callback should not be invoked if task was cancelled before delay")
                        return 42
                    }
                }

                if cancelDelayMs > 0 {
                    try await Task.sleep(nanoseconds: cancelDelayMs * 1_000_000)
                }
                task.cancel()

                let result = await task.result

                switch result {
                case .success(let v):
                    #expect(v == 42)
                case .failure(let e):
                    #expect(e is CancellationError, "Iteration \(i) failed with non-cancellation error: \(e)")
                }
            }
        }

        for try await _ in group {}
    }
}

@Test
func testFinallyCancelBeforeEnd() async throws {
    let task = Task<Int32, any Error>.detached {
        try await confirmation("Kotlin finally hook called", expectedCount: 1) { confirm in
            // Long delay so we can reliably cancel while suspended.
            return try await finallyDelayInt(delay: 3_000) {
                confirm() // must be called from Kotlin `finally`
            }
        }
    }

    try await Task.sleep(nanoseconds: 50_000_000)
    task.cancel()

    let result = await task.result
    #expect(result == .failure(CancellationError()))
}

@Test
func testFinallyCancelAfterEnd() async throws {
    let task = Task<Int32, any Error>.detached {
        try await confirmation("Kotlin finally hook called", expectedCount: 1) { confirm in
            return try await finallyDelayInt(delay: 20) {
                confirm()
            }
        }
    }

    try await Task.sleep(nanoseconds: 1_000_000_000)
    task.cancel()

    let result = await task.result
    #expect(result == .success(67))
}

@Test
func testDoubleCompletionDet_successThenSuccess() async throws {
    let v: Int32 = try await completeTwiceSuccessThenSuccessDeterministic()
    #expect(v == 42)
}

@Test
func testDoubleCompletionDet_successThenThrow() async throws {
    let v: Int32 = try await completeTwiceSuccessThenThrowDeterministic(message: "boom")
    #expect(v == 42)
}

@Test
func testDoubleCompletionDet_throwThenSuccess() async {
    let task = Task<Int32, any Error>.detached {
        try await completeTwiceThrowThenSuccessDeterministic(message: "first-throw")
    }

    let result = await task.result

    switch result {
    case .success:
        Issue.record("Expected failure, but got success")
    case .failure(let e):
        #expect(!(e is CancellationError))
        #expect(String(describing: e).contains("first-throw"))
    }
}

@Test
func testDoubleCompletionDet_cancelThenSuccess() async {
    let task = Task<Int32, any Error>.detached {
        try await completeTwiceCancelThenSuccessDeterministic()
    }

    let result = await task.result
    #expect(task.isCancelled)
    #expect(result == .failure(CancellationError()))
}

@Test
func testCallingKotlinLambdaThatUsesCoroutines() async throws {
    let block = testPrimitiveProducedLambda()
    try #expect(await block() == 42)
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