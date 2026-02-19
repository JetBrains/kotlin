import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testClashingWithAny() {
    assertEquals("description", TestClashingWithAny1().toString())
    assertEquals("toString", TestClashingWithAny1().toString_())
    assertEquals("toString_", TestClashingWithAny1().toString__())
    assertEquals(1, TestClashingWithAny1().hashCode())
    assertEquals(31, TestClashingWithAny1().hashCode_())
    assertFalse(TestClashingWithAny1().equals(TestClashingWithAny1()))
    assertTrue(TestClashingWithAny1().equals_(TestClashingWithAny1()))

    assertEquals("description", TestClashingWithAny2().toString())
    assertEquals(Unit, TestClashingWithAny2().toString_())
    assertEquals(2, TestClashingWithAny2().hashCode())
    assertEquals(Unit, TestClashingWithAny2().hashCode_())
    assertFalse(TestClashingWithAny2().equals(TestClashingWithAny2()))
    assertEquals(Unit, TestClashingWithAny2().equals_(42))

    assertEquals("description", TestClashingWithAny3().toString())
    assertEquals("toString:11", TestClashingWithAny3().toString(11))
    assertEquals(3, TestClashingWithAny3().hashCode())
    assertEquals(4, TestClashingWithAny3().hashCode(3))
    assertFalse(TestClashingWithAny3().equals(TestClashingWithAny3()))
    assertTrue(TestClashingWithAny3().equals())
}
