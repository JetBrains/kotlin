import kotlin.test.*

// Must never run in a consumer's binary: a plain -l library's @Test functions belong to the
// library's own test binaries only (pinned by CompilerTestRunnerTest.testWithLibrary).
// Written to fail on purpose so that an IC build that (incorrectly) registers it visibly
// fails instead of silently gaining a passing test.
@Test
fun libTest() {
    assertEquals("consumer-only", "library-test-ran")
}

fun libValue(): Int = 7
