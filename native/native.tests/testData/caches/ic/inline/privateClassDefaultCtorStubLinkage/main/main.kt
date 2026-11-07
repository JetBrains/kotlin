import kotlin.test.*

@Test
fun runTest() {
    // The inlined h() constructs C with its default value: 1. The IC link fails before this
    // runs (undefined C#<init>(Int, Int, DefaultConstructorMarker?)); a fixed IC prints 1.
    assertEquals(1, h())
}
