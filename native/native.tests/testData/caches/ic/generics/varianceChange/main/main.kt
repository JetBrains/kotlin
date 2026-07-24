import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(4, bar(ProducerImpl(), ConsumerImpl()))
}
