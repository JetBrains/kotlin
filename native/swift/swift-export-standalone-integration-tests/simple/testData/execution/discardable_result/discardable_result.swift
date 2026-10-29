import DiscardableResult
import Testing

@Test
func testDiscardableFunctionsCanBeCalledAsStatements() throws {
    calls = 0

    // Unannotated results get no @discardableResult in Swift, so this bare call only warns;
    // it stays here to pin that discarding an unspecified result still compiles and runs.
    discardable()
    #expect(calls == 1)

    let _ = checkable()
    #expect(calls == 2)
}

@Test
func testOverrides() throws {
    calls = 0

    SilentGreeter.shared.greet(name: "John")
    #expect(calls == 1)

    #expect(LoudGreeter().greet(name: "John") == "HELLO, John")
    #expect(calls == 2)

    let greeter: any Greeter = SilentGreeter.shared
    let _ = greeter.greet(name: "John")
    #expect(calls == 3)
}

@Test
func testMembers() throws {
    let counter = Counter()
    counter.increment()
    counter.increment()
    #expect(counter.value == 2)

    let one = Counter()
    one.increment()
    #expect((counter + one).value == 3)
}
