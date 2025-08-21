import LibraryA
import Testing

@Test
func test() throws {
    try #require(topLevelProperty == 42)
}
