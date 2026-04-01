import Main
import Testing
import Foundation

// MARK: - Basic type tests

@Test
func testSimple() async throws {
    let result = simple(arg: 8) { arg in
        await Task.yield()
        return arg + 42
    }

    try #require(result == 50)
}

@Test
func testWithLong() async throws {
    let result = withLong(arg: 1_000_000_000_000) { arg in
        await Task.yield()
        return arg * 2
    }

    try #require(result == 2_000_000_000_000)
}

@Test
func testWithDouble() async throws {
    let result = withDouble(arg: 3.14159) { arg in
        await Task.yield()
        return arg * 2.0
    }

    try #require(abs(result - 6.28318) < 0.0001)
}

@Test
func testWithBoolean() async throws {
    let resultTrue = withBoolean(arg: true) { arg in
        await Task.yield()
        return !arg
    }
    try #require(resultTrue == false)

    let resultFalse = withBoolean(arg: false) { arg in
        await Task.yield()
        return !arg
    }
    try #require(resultFalse == true)
}

@Test
func testWithString() async throws {
    let result = withString(arg: "Hello") { arg in
        await Task.yield()
        return arg + ", World!"
    }

    try #require(result == "Hello, World!")
}

@Test
func testWithEmptyString() async throws {
    let result = withString(arg: "") { arg in
        await Task.yield()
        return arg + "nonempty"
    }

    try #require(result == "nonempty")
}

@Test
func testWithAny() async throws {
    let result = withAny(arg: Foo.shared) { arg in
        await Task.yield()
        return arg
    }

    try #require(result as? Foo == Foo.shared)
}

@Test
func testWithObject() async throws {
    let result = withObject(arg: Foo.shared) { arg in
        await Task.yield()
        return arg
    }

    try #require(result == Foo.shared)
}

// MARK: - Optional/nullable type tests

@Test
func testWithOptionalIntNonNil() async throws {
    let result = withOptionalInt(arg: 42) { arg in
        await Task.yield()
        guard let value = arg else { return nil }
        return value + 1
    }

    try #require(result == 43)
}

@Test
func testWithOptionalIntNil() async throws {
    let result = withOptionalInt(arg: nil) { arg in
        await Task.yield()
        return arg ?? 100
    }

    try #require(result == 100)
}

@Test
func testWithOptionalIntReturningNil() async throws {
    let result = withOptionalInt(arg: 42) { _ in
        await Task.yield()
        return nil
    }

    try #require(result == nil)
}

@Test
func testWithOptionalStringNonNil() async throws {
    let result = withOptionalString(arg: "hello") { arg in
        await Task.yield()
        guard let value = arg else { return nil }
        return value.uppercased()
    }

    try #require(result == "HELLO")
}

@Test
func testWithOptionalStringNil() async throws {
    let result = withOptionalString(arg: nil) { arg in
        await Task.yield()
        return arg ?? "default"
    }

    try #require(result == "default")
}

@Test
func testWithOptionalObjectNonNil() async throws {
    let result = withOptionalObject(arg: Foo.shared) { arg in
        await Task.yield()
        return arg
    }

    try #require(result == Foo.shared)
}

@Test
func testWithOptionalObjectNil() async throws {
    let result = withOptionalObject(arg: nil) { arg in
        await Task.yield()
        return arg ?? Foo.shared
    }

    try #require(result == Foo.shared)
}

@Test
func testWithOptionalObjectReturningNil() async throws {
    let result = withOptionalObject(arg: Foo.shared) { _ in
        await Task.yield()
        return nil
    }

    try #require(result == nil)
}

// MARK: - Unit return type tests

@Test
func testWithUnitReturn() async throws {
    var sideEffect = 0
    let result = withUnitReturn(arg: 42) { arg in
        await Task.yield()
        sideEffect = Int(arg)
    }

    try #require(result == "completed")
    try #require(sideEffect == 42)
}

// MARK: - Multiple parameters tests

@Test
func testWithTwoParams() async throws {
    let result = withTwoParams(a: 10, b: "items") { a, b in
        await Task.yield()
        return "\(a) \(b)"
    }

    try #require(result == "10 items")
}

@Test
func testWithThreeParams() async throws {
    let result = withThreeParams(a: 5, b: "test", c: true) { a, b, c in
        await Task.yield()
        return "\(a)-\(b)-\(c)"
    }

    try #require(result == "5-test-true")
}

@Test
func testWithMixedParams() async throws {
    let result = withMixedParams(a: nil, b: "text", c: Foo.shared) { a, b, c in
        await Task.yield()
        let aStr = a.map { String($0) } ?? "nil"
        let cStr = c != nil ? "Foo" : "nil"
        return "\(aStr)-\(b)-\(cStr)"
    }

    try #require(result == "nil-text-Foo")
}

@Test
func testWithMixedParamsAllNil() async throws {
    let result = withMixedParams(a: nil, b: "", c: nil) { a, b, c in
        await Task.yield()
        let aStr = a.map { String($0) } ?? "nil"
        let cStr = c != nil ? "Foo" : "nil"
        return "\(aStr)-\(b)-\(cStr)"
    }

    try #require(result == "nil--nil")
}

// MARK: - No parameters tests

@Test
func testWithNoParams() async throws {
    let result = withNoParams {
        await Task.yield()
        return 42
    }

    try #require(result == 42)
}

@Test
func testWithNoParamsUnit() async throws {
    var called = false
    let result = withNoParamsUnit {
        await Task.yield()
        called = true
    }

    try #require(result == "completed")
    try #require(called == true)
}

// MARK: - Multiple closures tests

@Test
func testWithTwoClosures() async throws {
    let result = withTwoClosures(
        a: 10,
        first: { arg in
            await Task.yield()
            return arg * 2
        },
        second: { arg in
            await Task.yield()
            return arg + 5
        }
    )

    try #require(result == 25) // (10 * 2) + 5
}

@Test
func testWithClosureChain() async throws {
    let result = withClosureChain(
        initial: 1,
        closures: [
            { arg in await Task.yield(); return arg + 1 },
            { arg in await Task.yield(); return arg * 2 },
            { arg in await Task.yield(); return arg + 10 }
        ]
    )

    try #require(result == 14) // ((1 + 1) * 2) + 10
}

@Test
func testWithClosureChainEmpty() async throws {
    let result = withClosureChain(
        initial: 42,
        closures: []
    )

    try #require(result == 42) // No transformations
}

@Test
func testProduceClosureList() async throws {
    let closures = produceClosureList()
    try #require(closures.count == 3)

    var result: Int32 = 1
    for closure in closures {
        result = try await closure(result)
    }
    try #require(result == 14) // ((1 + 1) * 2) + 10
}

@Test
func testProduceNullableClosureList() async throws {
    let closures = produceNullableClosureList()
    try #require(closures.count == 2)

    var result: Int32 = 42
    for case .some(let closure) in closures {
        result = try await closure(result)
    }
    try #require(result == 42)
}

// MARK: - Nested/recursive calls tests

@Test
func testNestedCall() async throws {
    let result = nestedCall(depth: 3) { depth in
        await Task.yield()
        return depth * 10
    }

    // 3*10 + 2*10 + 1*10 + 0*10 = 60
    try #require(result == 60)
}

@Test
func testNestedCallZeroDepth() async throws {
    let result = nestedCall(depth: 0) { depth in
        await Task.yield()
        return 42
    }

    try #require(result == 42)
}

// MARK: - Exception handling tests

@Test
func testCatchingClosureSuccess() async throws {
    let result = catchingClosure {
        await Task.yield()
        return 42
    }

    try #require(result == "42")
}

@Test
func testCatchingClosureThrowing() async throws {
    let result = catchingClosure {
        await Task.yield()
        throw NSError(domain: "test", code: 1, userInfo: [NSLocalizedDescriptionKey: "test error"])
    }

    try #require(result.contains("caught"))
}

@Test
func testCatchingClosureWithArgSuccess() async throws {
    let result = catchingClosureWithArg(arg: 10) { arg in
        await Task.yield()
        return arg * 2
    }

    try #require(result == "20")
}

@Test
func testCatchingClosureWithArgThrowing() async throws {
    let result = catchingClosureWithArg(arg: 10) { _ in
        await Task.yield()
        throw NSError(domain: "test", code: 1, userInfo: [NSLocalizedDescriptionKey: "argument error"])
    }

    try #require(result.contains("caught"))
}

// MARK: - Cancellation tests

@Test
func testCallClosureWithDelayNoCancellation() async throws {
    let task = Task<Int32, any Error>.detached {
        return try await callClosureWithDelay(delayMs: 100) {
            await Task.yield()
            return 42
        }
    }

    let result = await task.result

    #expect(!task.isCancelled)
    #expect(result == .success(42))
}

@Test
func testCallClosureWithDelayCancelled() async throws {
    let task = Task<Int32, any Error>.detached {
        return try await callClosureWithDelay(delayMs: 2000) {
            await Task.yield()
            return 42
        }
    }

    // Cancel after a short delay
    DispatchQueue.global().asyncAfter(deadline: .now() + 0.1) {
        task.cancel()
    }

    let result = await task.result

    #expect(task.isCancelled)
    if case let .failure(e) = result {
        #expect(e is CancellationError)
    } else {
        Issue.record("Expected cancellation error")
    }
}

@Test
func testCallClosureCheckingCancellationNotCancelled() async throws {
    let task = Task<Int32, any Error>.detached {
        return try await callClosureCheckingCancellation {
            await Task.yield()
            return 42
        }
    }

    let result = await task.result

    #expect(!task.isCancelled)
    #expect(result == .success(42))
}

@Test
func testCallClosureCheckingCancellationPreCancelled() async throws {
    let task = Task<Int32, any Error>.detached {
        try await Task.sleep(nanoseconds: 1_000_000)
        return try await callClosureCheckingCancellation {
            Issue.record("Closure shouldn't be called when already cancelled")
            return 42
        }
    }
    task.cancel()

    let result = await task.result

    #expect(task.isCancelled)
    if case let .failure(e) = result {
        #expect(e is CancellationError)
    } else {
        Issue.record("Expected cancellation error")
    }
}

@Test
func testCallClosureInNewScope() async throws {
    let result = callClosureInNewScope {
        await Task.yield()
        return 42
    }

    try #require(result == 42)
}

// MARK: - Kotlin-initiated cancellation tests

@Test
func testKotlinCancelsSwiftClosure() async throws {
    let start = Date()
    let result = cancelClosureFromKotlin {
        // Sleep for 10 seconds - should be cancelled by Kotlin's withTimeoutOrNull after 100ms
        try await Task.sleep(nanoseconds: 10_000_000_000)
        return 42
    }

    let elapsed = Date().timeIntervalSince(start)
    try #require(result == "timed_out")
    // Should complete much faster than 10 seconds
    #expect(elapsed < 5.0)
}

@Test
func testKotlinCancelsSwiftClosureQuickReturn() async throws {
    // If the closure completes before the timeout, it should return normally
    let result = cancelClosureFromKotlin {
        await Task.yield()
        return 42
    }

    try #require(result == "completed: 42")
}

@Test
func testKotlinCancelsSwiftClosureWithArg() async throws {
    let start = Date()
    let result = cancelClosureWithArgFromKotlin(delayMs: 0) { arg in
        // Sleep for 10 seconds - should be cancelled by Kotlin's withTimeoutOrNull after 100ms
        try await Task.sleep(nanoseconds: 10_000_000_000)
        return arg * 2
    }

    let elapsed = Date().timeIntervalSince(start)
    try #require(result == "timed_out")
    #expect(elapsed < 5.0)
}

// MARK: - Closure that calls back into Kotlin

@Test
func testClosureCallingKotlin() async throws {
    let result = closureCallingKotlin { kotlinClosure in
        await Task.yield()
        let kotlinResult = try! await kotlinClosure()
        return kotlinResult + 8
    }

    try #require(result == 50) // 42 from Kotlin + 8
}

// MARK: - Edge cases

@Test
func testClosureCalledMultipleTimes() async throws {
    var callCount = 0
    let result = closureCalledMultipleTimes(times: 5) { i in
        await Task.yield()
        callCount += 1
        return i * 10
    }

    try #require(callCount == 5)
    try #require(result == 100) // 0 + 10 + 20 + 30 + 40
}

@Test
func testClosureCalledZeroTimes() async throws {
    var callCount = 0
    let result = closureCalledMultipleTimes(times: 0) { _ in
        await Task.yield()
        callCount += 1
        return 1
    }

    try #require(callCount == 0)
    try #require(result == 0)
}

@Test
func testClosureNeverCalledTrue() async throws {
    var called = false
    let result = closureNeverCalled(shouldCall: true) {
        await Task.yield()
        called = true
        return 42
    }

    try #require(called == true)
    try #require(result == 42)
}

@Test
func testClosureNeverCalledFalse() async throws {
    var called = false
    let result = closureNeverCalled(shouldCall: false) {
        await Task.yield()
        called = true
        return 42
    }

    try #require(called == false)
    try #require(result == -1)
}

// MARK: - Collection types

@Test
func testWithListParam() async throws {
    let result = withListParam(list: [1, 2, 3]) { list in
        await Task.yield()
        return list.map { $0 * 2 }
    }

    try #require(Array(result) == [2, 4, 6])
}

@Test
func testWithListParamEmpty() async throws {
    let result = withListParam(list: []) { list in
        await Task.yield()
        return list
    }

    try #require(result.isEmpty)
}

@Test
func testWithMapParam() async throws {
    let result = withMapParam(map: ["a": 1, "b": 2]) { map in
        await Task.yield()
        var newMap: [String: Int32] = [:]
        for (k, v) in map {
            newMap[k] = v * 10
        }
        return newMap
    }

    try #require(result["a"] == 10)
    try #require(result["b"] == 20)
}

@Test
func testWithMapParamEmpty() async throws {
    let result = withMapParam(map: [:]) { map in
        await Task.yield()
        return map
    }

    try #require(result.isEmpty)
}

// MARK: - Stress/concurrent tests

@Test
func testMultipleConcurrentClosureCalls() async throws {
    try await withThrowingTaskGroup(of: Int32.self) { group in
        for i: Int32 in 0..<10 {
            group.addTask {
                try await simpleSuspend(arg: i) { arg in
                    await Task.yield()
                    return arg * 2
                }
            }
        }

        var results: [Int32] = []
        for try await result in group {
            results.append(result)
        }

        #expect(results.sorted() == [0, 2, 4, 6, 8, 10, 12, 14, 16, 18])
    }
}

// MARK: - Helper for Result comparison

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

