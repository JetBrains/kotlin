import Vararg
import KotlinRuntime
import Testing

@Test
func testVararg() throws {
    let x = simple(a: "a", b: 4, 5, 6)
    try #require(x == "a456")

    let accessor = Accessor(x: 3.14, 2.72)
    try #require(accessor[0] > 3.1)
    try #require(accessor[1] < 2.8)
}
