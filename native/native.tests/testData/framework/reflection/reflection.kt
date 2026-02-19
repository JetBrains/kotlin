import kotlin.native.internal.reflect.objCNameOrNull
import kotlin.test.*

open class MyOpenClass

fun getMyOpenClassObjCName(): String {
    return MyOpenClass::class.objCNameOrNull!!
}

fun checkMyClass(instance: MyOpenClass): String {
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
    assertNull(kclass.objCNameOrNull)
    return "OK"
}