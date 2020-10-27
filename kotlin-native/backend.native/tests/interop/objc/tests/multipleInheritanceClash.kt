import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testMultipleInheritanceClash() {
    val clash1 = MultipleInheritanceClash1()
    val clash2 = MultipleInheritanceClash2()

    clash1.delegate = clash1
    assertEquals(clash1, clash1.delegate)
    clash1.setDelegate(clash2)
    assertEquals(clash2, clash1.delegate())

    clash2.delegate = clash1
    assertEquals(clash1, clash2.delegate)
    clash2.setDelegate(clash2)
    assertEquals(clash2, clash2.delegate())
}