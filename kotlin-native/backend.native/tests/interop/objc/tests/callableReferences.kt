import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testCallableReferences() {
    val createTestCallableReferences = ::TestCallableReferences
    assertEquals("<init>", createTestCallableReferences.name)
    val testCallableReferences: Any = createTestCallableReferences()
    assertTrue(testCallableReferences is TestCallableReferences)

    val valueRef: kotlin.reflect.KMutableProperty0<Int> = testCallableReferences::value
    assertEquals("value", valueRef.name)
    assertEquals(0, valueRef())
    valueRef.set(42)
    assertEquals(42, valueRef())

    val classMethodRef = (TestCallableReferences)::classMethod
    assertEquals("classMethod", classMethodRef.name)
    assertEquals(3, classMethodRef(1, 2))

    val instanceMethodRef = TestCallableReferences::instanceMethod
    assertEquals("instanceMethod", instanceMethodRef.name)
    assertEquals(42, instanceMethodRef(testCallableReferences))

    val boundInstanceMethodRef = testCallableReferences::instanceMethod
    assertEquals("instanceMethod", boundInstanceMethodRef.name)
    assertEquals(42, boundInstanceMethodRef())
}