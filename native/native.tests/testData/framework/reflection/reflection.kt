import kotlin.test.*

open class MyOpenClass

fun checkMyOpenClass(instance: MyOpenClass): String {
    val kclass = instance::class
    // isInstance
    assertTrue(MyOpenClass::class.isInstance(instance))
    assertTrue(kclass.isInstance(instance))
    assertFalse(kclass.isInstance(null))
    assertFalse(kclass.isInstance(MyOpenClass()))
    // names
    assertEquals("MyClass", kclass.simpleName)
    assertNull(kclass.qualifiedName)
    assertEquals("class swiftTestExecutable.MyClass", kclass.toString())
    return "OK"
}