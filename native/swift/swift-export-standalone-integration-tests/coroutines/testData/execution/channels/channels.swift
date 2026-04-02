import Main
import KotlinRuntime
import KotlinRuntimeSupport
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
func testNullable() async {
    let expected: [Elem?] = [Element1.shared, nil, Element2.shared, nil, Element3.shared]

    let task = Task<[Elem?], any Error>.detached {
        var actual: [Elem?] = []
        for try await element in testNullable() {
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
func testString() async {
    let expected: [String] = ["hello", "any", "world"]

    let task = Task<[String], any Error>.detached {
        var actual: [String] = []
        for try await element in testString() {
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
func testPrimitive() async {
    let expected: [UInt32] = [1, 2, 3]

    let task = Task<[UInt32], any Error>.detached {
        var actual: [UInt32] = []
        for try await element in testPrimitive() {
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
        String(describing: error).contains("Channel has Failed")
    }
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
