import kotlin.test.*
import test.*

@Test
fun runTest() {
    // The inlined g() constructs V with its default value: a fixed IC links and answers
    // "V(s=five)". The IC link fails before this runs (undefined
    // "test.V#<constructor>#static(kotlin.String?;kotlin.Int;DefaultConstructorMarker?)");
    // the same klibs link without incremental compilation.
    assertEquals("V(s=five)", g().toString())
}
