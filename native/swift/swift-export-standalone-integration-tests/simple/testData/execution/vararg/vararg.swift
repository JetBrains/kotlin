import Vararg
import KotlinRuntime
import Testing

@Test
func testVararg() throws {
    let x = simple(a: "a", b: produceCharArray())
    try #require(x == "abcd")
}
