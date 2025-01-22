import Main
import Testing

@Test
func testThrowingFunction() throws {
    try #require(throws: Error.self) {
        try throwingFunctionThatThrows(value: "error")
    }
}

@Test
func testThrowingConstructor() throws {
    try #require(throws: Error.self) {
        try NonConstructible(value: "error")
    }
}