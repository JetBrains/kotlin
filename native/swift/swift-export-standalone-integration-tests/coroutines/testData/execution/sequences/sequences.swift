import Main
import KotlinRuntime
import Testing
import Foundation

@Test
@MainActor
func testRegular() async {
    let expected: [Elem] = [Element1.shared, Element2.shared, Element3.shared]

    let task = Task<[Elem], any Error>.detached {
        var actual: [Elem] = []
        for try await element in testRegular() {
            actual.append(element)
        }
        return actual
    }

    let actual = await task.result

    #expect(!task.isCancelled)
    #expect(actual == .success(expected))
}

@Test
@MainActor
func testEmpty() async {
    let task = Task<Void, any Error>.detached {
        for try await _ in testEmpty() {
            throw CancellationError()
        }
    }

    let actual = await task.result

    #expect(!task.isCancelled)
    #expect(actual == .success(()))
}

@Test
@MainActor
func testFailing() async {
    let task = Task<Void, any Error>.detached {
        var iterator = testFailing().makeAsyncIterator()
        let first = try await iterator.next()
        #expect(first == Element1.shared)
        let second = try await iterator.next()
        #expect(second == Element2.shared)
        _ = try await iterator.next()
    }

    let actual = await task.result

    #expect(!task.isCancelled)
    #expect { try actual.get() } throws: { error in
        String(describing: error).contains("Flow has Failed")
    }
}

@Test
@MainActor
func testDiscarding() async {
    let discardingImmediately = Task<Void, any Error>.detached {
        var _ = testDiscarding().makeAsyncIterator()
    }
    let discardingImmediatelyResult = await discardingImmediately.result
    #expect(!discardingImmediately.isCancelled)
    #expect(discardingImmediatelyResult == .success(()))

    let discardingAtEnd = Task<Void, any Error>.detached {
        var iterator = testDiscarding().makeAsyncIterator()
        let first = try await iterator.next()
        #expect(first == Element1.shared)
        let second = try await iterator.next()
        #expect(second == Element2.shared)
        let third = try await iterator.next()
        #expect(third == Element3.shared)
    }
    let discardingAtEndResult = await discardingAtEnd.result
    #expect(!discardingAtEnd.isCancelled)
    #expect(discardingAtEndResult == .success(()))

    let discardingMidway = Task<Void, any Error>.detached {
        var iterator = testDiscarding().makeAsyncIterator()
        let first = try await iterator.next()
        #expect(first == Element1.shared)
    }
    let discardingMidwayResult = await discardingMidway.result
    #expect(!discardingMidway.isCancelled)
    #expect(discardingMidwayResult == .success(()))
}

@Test
@MainActor
func testStateFlow() async {
    let expected: [Elem] = [Element1.shared, Element2.shared, Element3.shared]

    let subject = CurrentSubject.shared

    let collectTask = Task<[Elem], any Error>.detached {
        var actual: [Elem] = []
        var i = 0;
        for try await element in subject.value {
            actual.append(element)
            i += 1
            guard i < 3 else { break }
        }
        return actual
    }

    let emitTask = Task<(), any Error>.detached {
        try await subject.update(value: Element1.shared)
        try await Task.sleep(nanoseconds: 300_000_000)
        try await subject.update(value: Element2.shared)
        try await Task.sleep(nanoseconds: 300_000_000)
        try await subject.update(value: Element3.shared)
    }

    let (emitResult, collectResult) = try await (emitTask.result, collectTask.result)

    #expect(!emitTask.isCancelled)
    #expect(!collectTask.isCancelled)
    #expect(emitResult == .success(()))
    #expect(collectResult == .success(expected))
}

func ==<T>(_ lhs: Result<T, any Error>, _ rhs: Result<T, any Error>) -> Bool where T: Equatable {
    switch (lhs, rhs) {
    case (.success(let l), .success(let r)): l == r
    case (.failure(let l), .failure(let r)): (l as any Equatable).equals(r)
    default: false
    }
}

func ==(_ lhs: Result<Void, any Error>, _ rhs: Result<Void, any Error>) -> Bool {
    switch (lhs, rhs) {
    case (.success, .success): true
    case (.failure(let l), .failure(let r)): (l as any Equatable).equals(r)
    default: false
    }
}

extension Equatable {
    func equals(_ other: Any) -> Bool {
        (other as? Self).map {
            self == $0
        } ?? false
    }
}