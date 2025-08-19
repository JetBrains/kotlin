import Main
import Testing

@Test
func testCallingKotlinThatUsesCoroutines() throws {
    try #require(demo() == 5)
}