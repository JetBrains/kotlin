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
